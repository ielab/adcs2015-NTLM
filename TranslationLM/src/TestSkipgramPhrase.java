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
public class TestSkipgramPhrase {

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
		
		int numtopterms = Integer.parseInt(args[6]);
		double mu = Double.parseDouble(args[7]);
		System.out.println("numtopterms set to " + numtopterms);
		System.setProperty("terrier.home", args[0]);
		Index index = Index.createIndex(args[1], "data");
		//Manager queryingManager = new Manager(index);
		System.out.println(index.getEnd());

		
		TranslationLMManager tlm_w2v_skipgram = new TranslationLMManager(index);
		tlm_w2v_skipgram.setTranslation("w2v");
		tlm_w2v_skipgram.setRarethreshold(index.getCollectionStatistics().getNumberOfDocuments()/200);
		tlm_w2v_skipgram.setTopthreshold(index.getCollectionStatistics().getNumberOfDocuments()/2);
		tlm_w2v_skipgram.setDirMu(mu);
		tlm_w2v_skipgram.setNumber_of_top_translation_terms(numtopterms);
		//tlm_w2v_skipgram.setRarethreshold(10);
		//tlm_w2v_skipgram.setTopthreshold(index.getCollectionStatistics().getNumberOfDocuments());
		//System.out.println("Translation thresholds: Lower=" + index.getCollectionStatistics().getNumberOfDocuments()/200 + "\t Upper=" + index.getCollectionStatistics().getNumberOfDocuments()/2);
		System.out.println("Initialise word2vec translation");
		//tlm_w2v.initialiseW2V("/Users/zuccong/experiments/sigir2015_nlm/vectors_wsj_skipgram_s200_w5_neg20_hs0_sam1e-4_iter5.txt");
		tlm_w2v_skipgram.initialiseW2V_atquerytime(args[4]);
		//tlm_w2v_skipgram.initialiseW2V_atquerytime("/Users/zuccong/experiments/sigir2015_nlm/vectors_wsj_skipgram_s200_w5_neg20_hs0_sam1e-4_iter5.txt");
		System.out.println("word2vec translation initialised");
	
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
		
		TRECDocnoOutputFormat TRECoutput_w2v_skipgram = new TRECDocnoOutputFormat(index);
		PrintWriter pt_w2v_skipgram = new PrintWriter(new File(args[3] + "_dir_w2v_skipgram_phrase.txt"));

		for(String qid : trecqueries.keySet()) {
			String query = trecqueries.get(qid);
			System.out.println(query + " - " + qid);

			
			System.out.println("Scoring with Dir TLM with w2v (skipgram)");
			//scoring with LM dir w2v
			Request rq_w2v = new Request();
			rq_w2v.setOriginalQuery(query);
			rq_w2v.setQueryID(qid);
			rq_w2v = tlm_w2v_skipgram.runMatching(rq_w2v, "w2v_phrase", "dir");
			TRECoutput_w2v_skipgram.printResults(pt_w2v_skipgram, rq_w2v, "dir_w2v_skipgram_phrase", "Q0", 1000);
			
		}
		pt_w2v_skipgram.flush();
		pt_w2v_skipgram.close();
		
	}
}
