import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import org.terrier.querying.Request;
import org.terrier.structures.Index;
import org.terrier.structures.outputformat.TRECDocnoOutputFormat;

/**
 * 
 */

/**
 * @author zuccong
 *
 */
public class TestMiSmethod {

	
	public static void main(String[] args) throws IOException, InterruptedException {
		System.out.println("Usage: ");
		System.out.println("args[0]: path to terrier.home");
		System.out.println("args[1]: path to index");
		System.out.println("args[2]: path to trec query file");
		System.out.println("args[3]: path to result file (including name of result file)");
		System.out.println("args[4]: path to skipgram vectors");
		System.out.println("args[5]: path to cbow vectors");
		System.out.println("args[6]: number of top translation terms");
		System.out.println("args[7]: value of mu");
		System.out.println("args[8]: value of s");
		
		int numtopterms = Integer.parseInt(args[6]);
		double mu = Double.parseDouble(args[7]);
		System.out.println("numtopterms set to " + numtopterms);
		System.setProperty("terrier.home", args[0]);
		Index index = Index.createIndex(args[1], "data");
		//Manager queryingManager = new Manager(index);
		System.out.println(index.getEnd());
		
		
		
		TranslationLMManager tlm_mi = new TranslationLMManager(index);
		tlm_mi.setTranslation("mi");
		//TODO: decomment these two lines - inserted only for medtrack
		//tlm_mi.setRarethreshold(index.getCollectionStatistics().getNumberOfDocuments()/200);
		//tlm_mi.setTopthreshold(index.getCollectionStatistics().getNumberOfDocuments()/2);
		tlm_mi.setRarethreshold(0);
		tlm_mi.setTopthreshold(1000000);
		tlm_mi.setDirMu(mu);
		tlm_mi.setNumber_of_top_translation_terms(numtopterms);
		tlm_mi.setAlpha(Double.parseDouble(args[8]));
		//tlm_mi.setRarethreshold(10);
		//tlm_mi.setTopthreshold(index.getCollectionStatistics().getNumberOfDocuments());
		//System.out.println("Translation thresholds: Lower=" + index.getCollectionStatistics().getNumberOfDocuments()/200 + "\t Upper=" + index.getCollectionStatistics().getNumberOfDocuments()/2);
		

		HashMap<String,String> trecqueries = new HashMap<String,String>();
		BufferedReader br = new BufferedReader(new FileReader(args[2]));
		String line = null;
		while ((line = br.readLine()) != null) {
			String[] input = line.split(" ");
			String qid = input[0];
			String query ="";
			for(int i=1; i<input.length;i++)
				query = query + " " + input[i];
			
			query = query.replaceAll("-", " ");
			query = query.replaceAll("\\p{Punct}", "");
			query = query.substring(1, query.length());
			trecqueries.put(qid, query.toLowerCase());
		}
		br.close();
		
		
		TRECDocnoOutputFormat TRECoutput_mi = new TRECDocnoOutputFormat(index);
		PrintWriter pt_mi = new PrintWriter(new File(args[3] + "_dir_mi_smethod_" + tlm_mi.alpha + ".txt"));
		
		for(String qid : trecqueries.keySet()) {
			String query = trecqueries.get(qid);
			System.out.println(query + " - " + qid);
			

			System.out.println("Scoring with Dir TLM with MI");
			//scoring with LM dir mi
			Request rq_mi = new Request();
			rq_mi.setOriginalQuery(query);
			rq_mi.setQueryID(qid);
			rq_mi = tlm_mi.runMatching(rq_mi, "mi_full_smethod", "dir");
			TRECoutput_mi.printResults(pt_mi, rq_mi, "dir_mi_full_smethod", "Q0", 1000);
			
		}

		pt_mi.flush();
		pt_mi.close();

	}
	
}
