import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
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
public class CleanDOTGOV {

	/**
	 * 
	 */
	public CleanDOTGOV() {
		// TODO Auto-generated constructor stub
	}
	
	/**
 	 * @param args
 	 * @throws IOException 
 	 */
 	public static void main(String[] args) throws IOException {

 		String dataPath = args[0];
 		String outputPath = args[1];

 		File folder = new File(dataPath);
 		File[] listOfFolders = folder.listFiles();
 		
 		
 		
 		
 		for(File subfolder : listOfFolders) {
 			File[] listOfFiles = subfolder.listFiles();
 			for (File file : listOfFiles) {
 	 			if (file.isFile() && !file.getName().startsWith(".") && file.getName().endsWith(".gz")) {
 	 				System.out.println("Processing " + file.getName());
 	 				GZIPInputStream gzip = new GZIPInputStream(new FileInputStream(file.getAbsolutePath()));
 	 				BufferedReader br = new BufferedReader(new InputStreamReader(gzip));
 	 				String line;
 	 				
 	 				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath + "/" + subfolder.getName() + "_" +  file.getName().replace("gz", "txt")), "utf-8"));
 	 				
 	 				boolean ignore=false;
 	 				String html="";
 	 				while ((line = br.readLine()) != null) {
 	 					boolean read = false;
 	 					
 	 					if(line.startsWith("<DOC>")){
 	 						html="";
 	 						//ignore
 	 						continue;
 	 					}
 	 					if(line.startsWith("</DOC>")) {
 	 						Document doc = Jsoup.parse(html);
 	 						Elements allElements = doc.getAllElements();
 	 						StringBuilder docString = new StringBuilder();
 	 						for (Element element : allElements) {
 	 						    for(TextNode tn : element.textNodes()){
 	 						    	if(!tn.text().isEmpty() || tn.text().equalsIgnoreCase(" ") || tn.text().equalsIgnoreCase(" \n"))
 	 						    		docString.append(tn.text().toLowerCase());// + "\n");
 	 						    }
 	 						}
 	 						String text = docString.toString();
 	 						if(!text.isEmpty())
 	 							writer.write(text + "\n");
 	 						continue;
 	 					}
 	 					
 	 					if(line.startsWith("<DOCNO>") ){
 	 						//ignore
 	 						continue;
 	 					}
 	 					if(line.startsWith("<DOCHDR>")){
 	 						//ignore and set the ignore flag to ignore the next lines
 	 						ignore=true;
 	 						continue;
 	 					}
 	 					if(line.startsWith("</DOCHDR>")){
 	 						//ignore but set the ignore flag to do not ignore to read the next lines
 	 						ignore=false;
 	 						continue;
 	 					}
 	 					if(!ignore && !line.isEmpty()) {
 	 						html= html.concat("\n"+line);
 	 						/*String convertedhtml = Jsoup.parse(line).text();
 	 						if(!convertedhtml.isEmpty())
 	 							writer.write(convertedhtml + "\n");*/
 	 					}
 	 				}
 	 				br.close();
 	 				writer.flush();
 					writer.close();
 	 			}

 	 		}
 		}
 		
 		
 	}


}
