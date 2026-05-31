package com.umc.linkyou.infra.gemini.prompt.curation;

public record CurationMentPrompt(String emotionName) {

    public String render() {
        return String.format("""
                사용자의 현재 감정은 '%s'입니다.
                이 감정을 기반으로, 해당 사용자에게 맞는 콘텐츠를 소개하는 큐레이션 멘트를 작성해주세요.

                [큐레이션 멘트 설명]
                - 상단 멘트는 큐레이션 페이지 가장 위에 노출되어, 사용자의 감정에 공감하며 관심을 끌어야 합니다.
                - 하단 멘트는 큐레이션 페이지 마지막에 노출되며, 콘텐츠를 마무리하면서 위로나 응원을 담아야 합니다.

                [작성 규칙]
                - 각 멘트는 반드시 한 문장으로 작성하세요.
                - 반드시 "(닉네임)"이라는 텍스트를 포함하세요. 이 표현은 절대로 바꾸지 마세요.
                - 출력 예시: {"header": "(닉네임)님, ...", "footer": "(닉네임)님, ..."}
                """,
                emotionName);
    }
}
