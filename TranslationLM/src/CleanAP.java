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
public class CleanAP {

	/**
	 * 
	 */
	public CleanAP() {
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

				if(line.startsWith("<DOC>") || line.startsWith("<DOCNO>") || line.contains("<FILEID>") || line.contains("<FIRST>") || line.contains("<SECOND>")
						|| line.contains("<HEAD>") || line.contains("<HEADLINE>") || line.contains("<BYLINE>") || line.contains("</BYLINE>") 
						|| line.contains("<DATELINE>") || line.contains("<HEAD>") || line.contains("</HEAD>")
						|| line.startsWith("</DOC>") || line.equalsIgnoreCase("\n")){
					//ignore
					continue;
				}
				String tmp= line.replace("<TEXT>", "");
				tmp = tmp.replace("</TEXT>", "");
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
