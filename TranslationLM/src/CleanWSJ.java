import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.zip.GZIPInputStream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

/**
 * 
 */

/**
 * @author zuccong
 *
 */
public class CleanWSJ {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		String dataPath = args[0];
		String outputPath = args[1];

		File file = new File(dataPath);
		System.out.println("Processing " + file.getName());
		BufferedReader br = new BufferedReader(new FileReader(file));
		String line;

		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath + "/" + file.getName().replace(".xml", "_cleaned.txt"))));

		StringBuilder docString = new StringBuilder();
		while ((line = br.readLine()) != null) {

			if(line.startsWith("<DOC>") || line.startsWith("<DOCNO>") || line.startsWith("<DD>") || line.startsWith("<SO>") || line.startsWith("<IN>")
					|| line.startsWith("<DATELINE>") || line.startsWith("<TEXT>") || line.startsWith("</TEXT>") || line.startsWith("</DOC>") || line.equalsIgnoreCase("\n")){
				//note some of the things in <IN> will be included
				//ignore
				continue;
			}
			if(line.startsWith("<HL>")) {
				String tmp= line.replace("<HL>", "");
				tmp=tmp.replace("</HL>", "");
				tmp = tmp.toLowerCase();
				tmp = tmp.replaceAll("\\d",""); //removes digits
				tmp = tmp.replaceAll("\\p{Punct}", " "); //removes punctuation
				tmp = tmp.replaceAll("\\s+", " "); //removes multiple spaces
				if(!tmp.isEmpty())
					docString.append(tmp + "\n");
			}else {
				//can only be text
				String tmp= line.toLowerCase();
				tmp= tmp.replace("<HL>", "");
				tmp = tmp.replaceAll("\\d",""); //removes digits
				tmp = tmp.replaceAll("\\p{Punct}", " "); //removes punctuation
				tmp = tmp.replaceAll("\\s+", " "); //removes multiple spaces
				if(!tmp.isEmpty())
					docString.append(tmp + "\n");
			}
		}
		String text = docString.toString();
		if(!text.isEmpty())
			writer.write(text + "\n");
		br.close();
		writer.flush();
		writer.close();
	}

}

