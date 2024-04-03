package ro;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class JCM {

    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final Pattern dataRegexPattern = Pattern.compile("messanger\\['gdata'\\] = (\\[.*?\\]);", Pattern.DOTALL);

    /**
     * Searches for people
     * @param query The search query
     * @return List of results
     */
    public static List<Person> search(String query) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://www.wikifeet.com/perl/ajax.fpl?req=suggest&gender=undefined&value=" + query))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        String responseBody = response.body();

        Document doc = Jsoup.parse(responseBody);
        List<Person> results = new ArrayList<>();
        for (Element element : doc.select("#suggestbox a")) {
            String name = element.select("div").text();
            String href = element.attr("href");
            String safeName = href.substring(href.lastIndexOf('/') + 1).replaceAll("_", "-");
            String url = "https://www.wikifeet.com/" + href;
            results.add(new Person(name, safeName, url));
        }
        for(Person person : results){
            System.out.println(person.getName());
        }
        return results;
    }

    /**
     * Gets links to all the thumbnails of a person's pictures.
     * @param person
     * @return Array of URLs
     */
    public static List<String> getThumbnails(Person person) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(person.getUrl()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        String responseBody = response.body();

        Matcher matcher = dataRegexPattern.matcher(responseBody);
        matcher.find();
        String jsonData = matcher.group(1);

        List<String> thumbnails = new ArrayList<>();
        JsonElement element = JsonParser.parseString(jsonData);
        if (element.isJsonArray()) {
            for (JsonElement img : element.getAsJsonArray()) {
                thumbnails.add("https://thumbs.wikifeet.com/" + img.getAsJsonObject().get("pid").getAsString() + ".jpg");
            }
        } else if (element.isJsonObject()) {
            JsonObject jsonObject = element.getAsJsonObject();
            thumbnails.add("https://thumbs.wikifeet.com/" + jsonObject.get("pid").getAsString() + ".jpg");
        }
        return thumbnails;
    }

    /**
     * Gets links to all of the person's pictures
     * @param person person info
     * @return List of URLs
     */
    public static List<String> getImages(Person person) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(person.getUrl()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        String responseBody = response.body();

        Matcher matcher = dataRegexPattern.matcher(responseBody);
        matcher.find();
        String jsonData = matcher.group(1);

        List<String> images = new ArrayList<>();
        JsonElement element = JsonParser.parseString(jsonData);
        if (element.isJsonArray()) {
            for (JsonElement img : element.getAsJsonArray()) {
                images.add("https://pics.wikifeet.com/" + person.getSafeName() + "-feet-" + img.getAsJsonObject().get("pid").getAsString() + ".jpg");
            }
        } else if (element.isJsonObject()) {
            JsonObject jsonObject = element.getAsJsonObject();
            images.add("https://pics.wikifeet.com/" + person.getSafeName() + "-feet-" + jsonObject.get("pid").getAsString() + ".jpg");
        }
        return images;
    }

    static class Person {
        private final String name;
        private final String safeName;
        private final String url;

        public Person(String name, String safeName, String url) {
            this.name = name;
            this.safeName = safeName;
            this.url = url;
        }

        public String getName() {
            return name;
        }

        public String getSafeName() {
            return safeName;
        }

        public String getUrl() {
            return url;
        }
    }
}