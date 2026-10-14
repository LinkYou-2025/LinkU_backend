import base64
import hashlib
import hmac
import json
import os
import time
import urllib.error
import urllib.parse
import urllib.request

import boto3

ec2 = boto3.client("ec2")

SESSION_TTL_SECONDS = 8 * 60 * 60


def _response(status_code, body):
    return {"statusCode": status_code, "body": json.dumps(body)}


def _b64url_encode(data):
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode()


def _b64url_decode(data):
    return base64.urlsafe_b64decode(data + "=" * (-len(data) % 4))


def _sign(signing_input):
    return hmac.new(os.environ["JWT_SECRET"].encode(), signing_input.encode(), hashlib.sha256).digest()


def _issue_jwt(claims):
    header = _b64url_encode(json.dumps({"alg": "HS256", "typ": "JWT"}).encode())
    payload = _b64url_encode(json.dumps(claims).encode())
    signing_input = f"{header}.{payload}"
    return f"{signing_input}.{_b64url_encode(_sign(signing_input))}"


def _verify_jwt(token):
    """서명과 만료를 확인하고 claims를 반환. 유효하지 않으면 None"""
    try:
        header, payload, signature = token.split(".")
        if not hmac.compare_digest(_b64url_decode(signature), _sign(f"{header}.{payload}")):
            return None
        claims = json.loads(_b64url_decode(payload))
    except ValueError:
        return None
    if claims.get("exp", 0) < time.time():
        return None
    return claims


def _session_claims(event):
    headers = event.get("headers") or {}
    scheme, _, token = headers.get("authorization", "").partition(" ")
    if scheme.lower() != "bearer" or not token:
        return None
    return _verify_jwt(token.strip())


def _http_json(url, data=None, headers=None):
    req = urllib.request.Request(url, data=data, headers={"Accept": "application/json", **(headers or {})})
    with urllib.request.urlopen(req, timeout=5) as res:
        return json.loads(res.read())


def _read_body(event):
    body = event.get("body") or "{}"
    if event.get("isBase64Encoded"):
        body = base64.b64decode(body).decode()
    try:
        return json.loads(body)
    except ValueError:
        return {}


def _github_unavailable():
    return _response(502, {"message": "GitHub에 연결할 수 없어요. 잠시 후 다시 시도해 주세요."})


def _login_with_github(event):
    code = _read_body(event).get("code")
    if not isinstance(code, str) or not code:
        return _response(400, {"message": "code가 필요해요."})

    try:
        token_res = _http_json(
            "https://github.com/login/oauth/access_token",
            data=urllib.parse.urlencode(
                {
                    "client_id": os.environ["GITHUB_CLIENT_ID"],
                    "client_secret": os.environ["GITHUB_CLIENT_SECRET"],
                    "code": code,
                }
            ).encode(),
        )
    except (urllib.error.URLError, TimeoutError):
        return _github_unavailable()
    access_token = token_res.get("access_token")
    if not access_token:
        return _response(400, {"message": "GitHub 로그인에 실패했어요. 다시 시도해 주세요."})

    org = os.environ["GITHUB_ORG"]
    try:
        membership = _http_json(
            f"https://api.github.com/user/memberships/orgs/{org}",
            headers={"Authorization": f"Bearer {access_token}", "User-Agent": "linku-server-switch"},
        )
    except urllib.error.HTTPError as e:
        if e.code in (403, 404):
            return _response(403, {"message": f"{org} 조직 멤버만 로그인할 수 있어요."})
        return _github_unavailable()
    except (urllib.error.URLError, TimeoutError):
        return _github_unavailable()
    if membership.get("state") != "active":
        return _response(403, {"message": f"{org} 조직 멤버만 로그인할 수 있어요."})

    user = {"login": membership["user"]["login"], "avatarUrl": membership["user"]["avatar_url"]}
    now = int(time.time())
    token = _issue_jwt({"sub": user["login"], "iat": now, "exp": now + SESSION_TTL_SECONDS})
    return _response(200, {"token": token, "user": user})


def lambda_handler(event, context):
    route_key = event.get("routeKey", "")

    if route_key == "POST /auth/github":
        return _login_with_github(event)

    if _session_claims(event) is None:
        return _response(401, {"message": "unauthorized"})

    instance_id = os.environ["INSTANCE_ID"]

    if route_key == "GET /status":
        instance = ec2.describe_instances(InstanceIds=[instance_id])["Reservations"][0]["Instances"][0]
        return _response(200, {"state": instance["State"]["Name"]})

    if route_key == "POST /stop":
        ec2.stop_instances(InstanceIds=[instance_id])
        return _response(200, {"message": "stopping"})

    if route_key == "POST /start":
        ec2.start_instances(InstanceIds=[instance_id])
        return _response(200, {"message": "starting"})

    return _response(404, {"message": "not found"})
