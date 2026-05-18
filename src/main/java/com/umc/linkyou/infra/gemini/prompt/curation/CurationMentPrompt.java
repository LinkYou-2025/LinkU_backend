package com.umc.linkyou.infra.gemini.prompt.curation;

public class CurationMentPrompt {

    private final String emotionName;

    public CurationMentPrompt(String emotionName) {
        this.emotionName = emotionName;
    }

    public String render() {
        return String.format(
                "사용자의 현재 감정은 '%s'입니다.\n" +
                "이 감정을 기반으로, 해당 사용자에게 맞는 콘텐츠를 소개하는 큐레이션 멘트를 작성해주세요.\n\n" +
                "[큐레이션 멘트 설명]\n" +
                "- 상단 멘트는 큐레이션 페이지 가장 위에 노출되어, 사용자의 감정에 공감하며 관심을 끌어야 합니다.\n" +
                "- 하단 멘트는 큐레이션 페이지 마지막에 노출되며, 콘텐츠를 마무리하면서 위로나 응원을 담아야 합니다.\n\n" +
                "[작성 규칙]\n" +
                "- 각 멘트는 반드시 한 문장으로 작성하세요.\n" +
                "- 반드시 \"(닉네임)\"이라는 텍스트를 포함하세요. 이 표현은 절대로 바꾸지 마세요.\n" +
                "- 아래 형식의 JSON 형태로만 출력하세요:\n" +
                "{\n" +
                "  \"header\": \"(닉네임)님, ...\",\n" +
                "  \"footer\": \"(닉네임)님, ...\"\n" +
                "}\n\n" +
                "※ JSON 외에는 아무것도 출력하지 말고, (닉네임)이라는 문자열을 절대로 수정하지 마세요.",
                emotionName
        );
    }
}
