package bot;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Main {
    public static void main(String[] args) {
        System.out.println("こんにちは！LINE Botのプログラムが起動したよ！");
        
        // 1. 天気APIから小樽の天気を取得する
        String weatherInfo = getOtaruWeather();
        System.out.println("取得した天気情報:\n" + weatherInfo);
        
        // 2. 取得した天気をLINEに送信する
        sendLineMessage(weatherInfo);
    }

    // LINEにメッセージを送信するメソッド
    private static void sendLineMessage(String weatherText) {
        // GitHub Actionsの「Secrets（環境変数）」からトークンとIDを読み込むように変更！
        // こうすることで、パスワードを直接コードに書かなくて済むから安全だよ👍
        String channelToken = System.getenv("LINE_CHANNEL_TOKEN");
        String userId = System.getenv("LINE_USER_ID");

        if (channelToken == null || userId == null) {
            System.out.println("❌ エラー: LINEのトークンかユーザーIDが設定されていません！");
            System.out.println("GitHubのSecrets設定を確認してね！");
            return;
        }

        String url = "https://api.line.me/v2/bot/message/push";

        // LINEに送るJSONデータを作る（改行が含まれていても大丈夫なようにエスケープ処理を入れる）
        String escapedText = weatherText.replace("\n", "\\n");
        String jsonBody = String.format(
            "{\"to\": \"%s\", \"messages\": [{\"type\": \"text\", \"text\": \"%s\"}]}",
            userId, escapedText
        );

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + channelToken)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            // 送信！
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                System.out.println("✅ LINEへの送信成功！スマホを確認してみて！");
            } else {
                System.out.println("❌ LINE送信エラー: " + response.statusCode());
                System.out.println("エラー内容: " + response.body());
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("LINEの送信に失敗しちゃった💦");
        }
    }

    private static String getOtaruWeather() {
        // 小樽市の緯度・経度
        String lat = "43.1907";
        String lon = "141.0023";
        // Open-Meteo APIのURL (今日の天気コード、最高・最低気温を取得)
        String url = "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon + "&daily=weathercode,temperature_2m_max,temperature_2m_min&timezone=Asia%2FTokyo";

        try {
            // HTTPクライアントを作ってAPIにリクエストを送信！
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            // 取得したJSONデータをGsonで読み解く
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonObject daily = json.getAsJsonObject("daily");
            
            // 今日のデータ（配列の0番目）を抜き出す
            double maxTemp = daily.getAsJsonArray("temperature_2m_max").get(0).getAsDouble();
            double minTemp = daily.getAsJsonArray("temperature_2m_min").get(0).getAsDouble();
            int weatherCode = daily.getAsJsonArray("weathercode").get(0).getAsInt();
            
            // 天気コードを分かりやすい日本語に変換
            String weatherText = decodeWeather(weatherCode);

            return String.format("今日の小樽の天気は「%s」！\n🌡️ 最高気温: %.1f℃ / 最低気温: %.1f℃ だよ！", weatherText, maxTemp, minTemp);

        } catch (Exception e) {
            e.printStackTrace();
            return "天気の取得に失敗しちゃったみたい💦";
        }
    }

    // WMO天気コードを簡単な日本語に変換するメソッド
    private static String decodeWeather(int code) {
        if (code == 0) return "快晴 ☀️";
        if (code >= 1 && code <= 3) return "晴れ時々くもり 🌤️";
        if (code >= 45 && code <= 48) return "霧 🌫️";
        if (code >= 51 && code <= 67) return "雨 ☔️";
        if (code >= 71 && code <= 77) return "雪 ⛄️";
        if (code >= 80 && code <= 82) return "にわか雨 🌦️";
        if (code >= 85 && code <= 86) return "雪 ❄️";
        if (code >= 95) return "雷雨 ⚡️";
        return "よくわからない天気 🤔";
    }
}
