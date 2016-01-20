package intnet;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.crypto.Data;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import application.FXController;

public class Connection {
	
	static Charset asciiEncoder = Charset.forName("US-ASCII");
	
	public static String googleSearchQueryResults () throws IOException{

		final String usersIPAddres = fetchIpFromAmazon();

		URL url = new URL(
				"https://ajax.googleapis.com/ajax/services/search/web?v=1.0&"
						+ "q="
						+ FXController.bandArtist.replace(" ", "%20")
						+ "%20"
						+ FXController.songTitle.replace(" ", "%20")
						+ "%20site:pleer.com"
						+ "&userip="
						+ usersIPAddres);
		
		System.out.println(url.toString());
		
		URLConnection connection = url.openConnection();
		connection.addRequestProperty("Referer", "www.referrer.com");

		String line;
		StringBuilder builder = new StringBuilder();
		BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		while((line = reader.readLine()) != null) {
			builder.append(line);
		}

		JSONObject json = new JSONObject(builder.toString()).getJSONObject("responseData");


		String pleerSearch;

		JSONArray websiteLinksArray = (JSONArray) json.get("results");

		pleerSearch = (websiteLinksArray.getJSONObject(0).getString("url"));
		
		return pleerSearch;
	}
	
	public static List<String> googleImageSearchQueryResults () throws IOException{

		if(FXController.albumTitle != FXController.songTitle) {
			FXController.albumTitle = FXController.albumTitle.replace("\"", "");
			FXController.albumYear = FXController.albumTitle.substring(FXController.albumTitle.length()-5, FXController.albumTitle.length()-1);
			FXController.albumTitle = FXController.albumTitle.substring(0, FXController.albumTitle.length()-7);	
		}
		
		FXController.imageURLs.clear();
		URL url = new URL(
				"https://www.googleapis.com/customsearch/v1?key=AIzaSyCHoGF1u8RytvaNeCk69iyD9ouwFBmsndQ&cx=007547444199860528947:flga7fapbrm&q="
						+ FXController.bandArtist.replace(" ", "%20")
						+ FXController.albumTitle.replace(" ", "%20")
						+ "%20"
						+ FXController.albumYear.replace(" ", "%20")
						+ "%20album%20cover%20art"
						+ "&userip=&searchType=image&alt=json");
		
		if(FXController.albumTitle == FXController.songTitle) {
			FXController.albumTitle = "";
		}
		
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        BufferedReader br = new BufferedReader(new InputStreamReader(
                (conn.getInputStream())));
        
        String output;
        while ((output = br.readLine()) != null) {
            if(output.contains("\"link\":")) {
            	FXController.imageURLs.add(output.substring(12, output.length()-2));
            }
        }
        
        conn.disconnect();

		return FXController.imageURLs;
	}

	public static List<String> getSongLyricsFromAZLyrics(String fullURLPath) throws IOException {

		List<String> lyrics = new ArrayList<String>();		
		Document doc = Jsoup.connect(fullURLPath).get();
		String title = titleFixer(doc.title());
		
		FXController.songFullTitle = title;
		
		String[] breakTitle = FXController.songFullTitle.split(" - ");
		FXController.bandArtist = breakTitle[0];
		FXController.songTitle = breakTitle[1];
		
		// if album title not shown on azlyrics, set album title to blank
		if(doc.select("div.album-panel").size() != 0) {
			Element q = doc.select("div.album-panel").get(0).child(1);
			FXController.albumTitle = q.text();
		}
		else {
			FXController.albumTitle = FXController.songTitle;
		}

		// get each line
		Element p = doc.select("div").get(22);
		for(Node e: p.childNodes()) {
			if(e instanceof TextNode) {
				lyrics.add(((TextNode)e).text() + "\n");
			}
		}
		// first line is a white space, remove it
		lyrics.remove(0);

		return lyrics;
	}
	
	public static void getSongFromPleer() throws IOException {
				
		FXController.bandArtist = FXController.bandArtist.replaceAll("\\bfeat\\b", "");
		FXController.songTitle = FXController.songTitle.replaceAll("\\bfeat\\b", "");
		FXController.bandArtist = FXController.bandArtist.replaceAll("\\(.*\\)","");//.replaceAll("[!@#$%^&*(){}:\"<>?]", "");
		FXController.songTitle = FXController.songTitle.replaceAll("\\(.*\\)","");//.replaceAll("[!@#$%^&*(){}:\"<>?]", "");
		FXController.bandArtist = deAccent(FXController.bandArtist);
		FXController.songTitle = deAccent(FXController.songTitle);

		String fullURLPath = "http://www.pleer.com/browser-extension/search?q=" + FXController.bandArtist.replace(" ", "+").replaceAll("[!@#$%^&*(){}:\"<>?]", "") + "+" +FXController.songTitle.replace(" ", "+").replaceAll("[!@#$%^&*(){}:\"<>?]", "");;
		
		System.out.println(fullURLPath);
		
		URL url = new URL(fullURLPath);
		HttpURLConnection request = (HttpURLConnection) url.openConnection();
		request.connect();
		
		JsonParser jp = new JsonParser();
		JsonElement root = jp.parse(new InputStreamReader((InputStream) request.getContent()));
		JsonObject rootobj = root.getAsJsonObject();
		JsonArray arr = rootobj.getAsJsonArray("tracks");
		try {
			rootobj = arr.get(0).getAsJsonObject();
		} catch (IndexOutOfBoundsException e) {
			System.out.println("no pleer search");
		}
		
		List<String> fileList = new ArrayList<String>();
		List<String> fullTitleList = new ArrayList<String>();
		
		FXController.fileList = fileList;
		Pattern p = Pattern.compile("^[\\x20-\\x7d]*$");
		
		for(int i = 0; i < arr.size(); i++) {
			rootobj = arr.get(i).getAsJsonObject();
			Matcher m1 = p.matcher(rootobj.get("artist").toString().replace("\"", "").replace("[", "~").replace("]", "~"));
			Matcher m2 = p.matcher(rootobj.get("track").toString().replace("\"", "").replace("[", "~").replace("]", "~"));
			Matcher m3 = Pattern.compile("rework", Pattern.CASE_INSENSITIVE).matcher(rootobj.get("track").toString());
			Matcher m4 = Pattern.compile("remix", Pattern.CASE_INSENSITIVE).matcher(rootobj.get("track").toString());
			Matcher m5 = Pattern.compile("rework", Pattern.CASE_INSENSITIVE).matcher(rootobj.get("track").toString());
			Matcher m6 = Pattern.compile("cover", Pattern.CASE_INSENSITIVE).matcher(rootobj.get("track").toString());
			
			if(m1.find() && m2.find() && !m3.find() && !m4.find() && !m5.find() && !m6.find()) {
				fileList.add(rootobj.get("file").toString().replace("\"", ""));
				fullTitleList.add(rootobj.get("artist").toString().replace("\"", "") + " - " + rootobj.get("track").toString().replace("\"", ""));
			}
		}
		
		FXController.fullTitleList = fullTitleList;
	}

	public static void getiTunesSongInfo(String songInfoQuery) throws IOException {

		String fullURLPath = "https://itunes.apple.com/search?term=" + songInfoQuery.replace(" ", "+");
		
		System.out.println("!" + fullURLPath.toString());
		
		URL url = new URL(fullURLPath);
		HttpURLConnection request = (HttpURLConnection) url.openConnection();
		request.connect();
		
		JsonParser jp = new JsonParser();
		JsonElement root = jp.parse(new InputStreamReader((InputStream) request.getContent()));
		JsonObject rootobj = root.getAsJsonObject();
		JsonArray arr = rootobj.getAsJsonArray("results");
		try {
			rootobj = arr.get(0).getAsJsonObject();
		} catch (IndexOutOfBoundsException e) {
			System.out.println("not in itunes");
		}
		
		System.out.println(arr);
		
		int itunesIndex = 0;
		String test = "";
		do {
			try {
			rootobj = arr.get(itunesIndex).getAsJsonObject();
			} catch (IndexOutOfBoundsException e) {
				System.out.println("not in itunes + searched all possible");
			}
			test = (rootobj.get("kind").toString().replace("\"", ""));
			itunesIndex++;
		} while(!test.equals("song"));
		
		itunesIndex--;
		
		// parse artist
		rootobj = arr.get(itunesIndex).getAsJsonObject();
		FXController.bandArtist = (rootobj.get("artistName").toString().replace("\"", ""));
		
		// parse song title
		rootobj = arr.get(itunesIndex).getAsJsonObject();
		FXController.songTitle = (rootobj.get("trackName").toString().replace("\"", ""));
		
		// parse album title
		rootobj = arr.get(itunesIndex).getAsJsonObject();
		FXController.albumTitle = (rootobj.get("collectionName").toString().replace("\"", ""));
		
		// parse year
		rootobj = arr.get(itunesIndex).getAsJsonObject();
		FXController.albumYear = (rootobj.get("releaseDate").toString().replace("\"", "").substring(0, 4));
		
		// parse genre
		rootobj = arr.get(itunesIndex).getAsJsonObject();
		FXController.genre = (rootobj.get("primaryGenreName").toString().replace("\"", ""));
		
		// parse cover art
		rootobj = arr.get(itunesIndex).getAsJsonObject();
		FXController.coverArtUrl = (rootobj.get("artworkUrl100").toString().replace("\"", "").replace("100x100bb.jpg", "500x500bb.jpg"));
		
		FXController.songFullTitle = FXController.bandArtist + " - " + FXController.songTitle;
		
	}
	
	private static String fetchIpFromAmazon() throws IOException {
		URL url = null;
		url = new URL("http://checkip.amazonaws.com/");
		BufferedReader br = null;
		br = new BufferedReader(new InputStreamReader(url.openStream()));
		try {
			return br.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return "Failed to get IP from Amazon";
	}
	
	private static String titleFixer(String title) {
		title = title.substring(0, title.length());
		title = title.toLowerCase();
		title = toTitleCase(title);
		
		title = title.replace("Lyrics", "");
		title = title.replace("  ", " ");
		
		return title;
	}
	
	private static String toTitleCase(String givenString) {
	    String[] arr = givenString.split(" ");
	    StringBuffer sb = new StringBuffer();

	    for (int i = 0; i < arr.length; i++) {
	        sb.append(Character.toUpperCase(arr[i].charAt(0)))
	            .append(arr[i].substring(1)).append(" ");
	    }          
	    return sb.toString().trim();
	}  
	
	private static String deAccent(String str) {
	    String nfdNormalizedString = Normalizer.normalize(str, Normalizer.Form.NFD); 
	    Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
	    return pattern.matcher(nfdNormalizedString).replaceAll("");
	}
}
