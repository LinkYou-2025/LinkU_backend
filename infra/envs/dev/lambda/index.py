import json
import os

import boto3

ec2 = boto3.client("ec2")


def _response(status_code, body):
    return {"statusCode": status_code, "body": json.dumps(body)}


def lambda_handler(event, context):
    query = event.get("queryStringParameters") or {}

    expected_secret = os.environ["SWITCH_SECRET"]
    if query.get("secret") != expected_secret:
        return _response(403, {"message": "forbidden"})

    route_key = event.get("routeKey", "")
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
