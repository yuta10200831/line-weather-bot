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
        // GitHub Actionsの「Secrets（環境変数）」からトークンを読み込む
        // ブロードキャスト（全員送信）にするから、特定のユーザーIDは不要になるよ！
        String channelToken = System.getenv("LINE_CHANNEL_TOKEN");

        if (channelToken == null) {
            System.out.println("❌ エラー: LINEのトークンが設定されていません！");
            System.out.println("GitHubのSecrets設定を確認してね！");
            return;
        }

        // ブロードキャスト（一斉送信）用のURLに変更！
        String url = "https://api.line.me/v2/bot/message/broadcast";

        // LINEに送るJSONデータを作る（宛先の "to" が不要になるよ！）
        String escapedText = weatherText.replace("\n", "\\n");
        String jsonBody = String.format(
            "{\"messages\": [{\"type\": \"text\", \"text\": \"%s\"}]}",
            escapedText
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

    // ランダムなラッキーアイテムを返すメソッド
    private static String getRandomLuckyItem() {
        String[] items = {
            "あたたかいコーヒー ☕️",
            "お気に入りの音楽 🎵",
            "赤いボールペン 🖊️",
            "甘いチョコレート 🍫",
            "新しい靴下 🧦",
            "読みかけの本 📖",
            "笑顔 ☺️",
            "お散歩 🚶‍♂️",
            "フルーツ 🍎",
            "くりぽこちゃん!!"
        };
        java.util.Random random = new java.util.Random();
        return items[random.nextInt(items.length)];
    }

    // ランダムな挨拶を返すメソッド
    private static String getRandomGreeting() {
        String[] greetings = {
            "おはよう！☀️",
            "今日も1日がんばろう！💪",
            "気をつけていってらっしゃい！👋",
            "素敵な1日になりますように✨",
            "今日も元気に行ってみよう！😆",
            "朝だよ！起きる時間だー！⏰",
            "僕もそろそろ起きようかな",
            "なんかお腹すいたよー"
        };
        java.util.Random random = new java.util.Random();
        return greetings[random.nextInt(greetings.length)];
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
            
            // 明日のデータ（配列の1番目）も抜き出す
            int tomorrowWeatherCode = daily.getAsJsonArray("weathercode").get(1).getAsInt();
            
            // 天気コードを分かりやすい日本語に変換
            String weatherText = decodeWeather(weatherCode);
            String tomorrowWeatherText = decodeWeather(tomorrowWeatherCode);
            
            // 気温から服装アドバイスを取得
            String clothingAdvice = getClothingAdvice(maxTemp);
            
            // ランダムな挨拶を取得
            String greeting = getRandomGreeting();

            // ランダムなラッキーアイテムを取得
            String luckyItem = getRandomLuckyItem();

            return String.format("%s\n\n今日の小樽の天気は「%s」！\n🌡️ 最高気温: %.1f℃ / 最低気温: %.1f℃ だよ！\n\n👕 服装アドバイス:\n%s\n\n🔮 今日のラッキーアイテム: 「%s」\n\n💡 ちなみに明日は「%s」の予報だよ！", 
                greeting, weatherText, maxTemp, minTemp, clothingAdvice, luckyItem, tomorrowWeatherText);

        } catch (Exception e) {
            e.printStackTrace();
            return "天気の取得に失敗しちゃったみたい💦";
        }
    }

    // 気温に合わせた服装アドバイスを返すメソッド
    private static String getClothingAdvice(double maxTemp) {
        if (maxTemp >= 25.0) {
            return "今日は半袖で快適に過ごせそう！👕";
        } else if (maxTemp >= 15.0) {
            return "長袖シャツか、薄手の上着があるといいかも！🧥";
        } else if (maxTemp >= 5.0) {
            return "結構冷えるから、暖かいコートを着ていってね！🧣";
        } else {
            return "極寒！ダウンジャケットと手袋必須だよ！🥶";
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
