import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;

/**
 * 
 */

/**
 * @author zuccong
 *
 */
public class CleanSJM {

	/**
	 * 
	 */
	public CleanSJM() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) throws IOException {
		String dataPath = args[0];
		String outputPath = args[1];
		
		File folder = new File(dataPath);
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath)));
		
		
		for (File file : folder.listFiles()) {
			System.out.println("Processing " + file.getName());
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line;

			
			StringBuilder docString = new StringBuilder();
			while ((line = br.readLine()) != null) {

				if(line.startsWith("<DOC>") || line.startsWith("<DOCNO>") || line.startsWith("<ACCESS>") || line.startsWith("<DESCRIPT>") || line.startsWith("<LEADPARA>")
						|| line.startsWith("<SECTION>") || line.startsWith("<HEADLINE>") || line.startsWith("<MEMO>") 
						|| line.startsWith("<BYLINE>") || line.startsWith("<COUNTRY>")  || line.startsWith("<EDITION>")  || line.startsWith("<CODE>")  || line.startsWith("<NAME>") 
						|| line.startsWith("<PUBDATE>") || line.startsWith("<DAY>")  || line.startsWith("<MONTH>")  || line.startsWith("<PG.COL>")  || line.startsWith("<PUBYEAR>")
						|| line.startsWith("<REGION>") || line.startsWith("<STATE>")  || line.startsWith("<WORD.CT>")  || line.startsWith("<DATELINE>")  || line.contains("</DATELINE>")
						|| line.startsWith("<COPYRGHT>") || line.startsWith("<LIMLEN>")  || line.startsWith("<LANGUAGE>")
						|| line.startsWith("</DOC>") || line.equalsIgnoreCase("\n")){
					//note some of the things in <IN> will be included
					//ignore
					continue;
				}
				String tmp= line.replace("<TEXT>", "");
				tmp = tmp.replace("</TEXT>", "");
				tmp = tmp.replace("<CAPTION>", "");
				tmp = tmp.replace("</CAPTION>", "");
				tmp = tmp.replace("&equals;", "");
				tmp = tmp.toLowerCase();
				tmp = tmp.replaceAll("\\d",""); //removes digits
				tmp = tmp.replaceAll("\\p{Punct}", " "); //removes punctuation
				tmp = tmp.replaceAll("\\s+", " "); //removes multiple spaces
				if(!tmp.isEmpty())
					docString.append(tmp + "\n");
			}
			String text = docString.toString();
			if(!text.isEmpty())
				writer.write(text + "\n");
			br.close();

		}
		writer.flush();
		writer.close();

	}



}
