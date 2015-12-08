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
public class MakeCorpusEnwik9 {
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

		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath + "/" + file.getName() + "_trecformat.txt")));

		StringBuilder docString = new StringBuilder();
		int docid=0;
		while ((line = br.readLine()) != null) {

			if(line.contains("<page>")) {
				docString.append("<DOC>\n<DOCNO>" + docid + "</DOCNO>\n");
				docid = docid + 1;
				continue;
			}
			if(line.contains("</page>")) {
				docString.append("</DOC>\n");
				continue;
			}
			if(line.contains("<title>")){
				//this is a new document 
				String tmp = line.replace("<title>", "<TITLE>");
				tmp = line.replace("</title>", "</TITLE>");
				docString.append(tmp + "\n");
				continue;
			}
			if(line.contains("<id>") || line.contains("<revision>") || line.contains("<timestamp>") || line.contains("<contributor>")
					|| line.contains("<username>") || line.contains("<minor />") || line.contains("</contributor>") || line.contains("</revision>")
					|| line.contains("<comment>") || line.contains("<#REDIRECT>")
					) {
				//ignore the line
				continue;
			}else {
				 docString.append(line + "\n"); 
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
