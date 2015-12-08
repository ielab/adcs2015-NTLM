import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Properties;
import java.util.Vector;

import org.terrier.applications.batchquerying.QuerySource;
import org.terrier.applications.batchquerying.TRECQuery;
import org.terrier.matching.ResultSet;
import org.terrier.querying.Manager;
import org.terrier.querying.Request;
import org.terrier.querying.SearchRequest;
import org.terrier.structures.Index;
import org.terrier.structures.SingleLineTRECQuery;
import org.terrier.structures.outputformat.TRECDocnoOutputFormat;


/**
 * 
 */

/**
 * @author zuccong
 *
 */
public class TestRetrieval {

	/**
	 * This is the main method that execute a test retrieval
	 * 
	 * 
	 * @param args
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
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
		TranslationLMManager tlm = new TranslationLMManager(index);
		tlm.setTranslation("null");
		tlm.setDirMu(mu);
		
		TranslationLMManager tlm_mi = new TranslationLMManager(index);
		tlm_mi.setTranslation("mi");
		tlm_mi.setRarethreshold(index.getCollectionStatistics().getNumberOfDocuments()/200);
		tlm_mi.setTopthreshold(index.getCollectionStatistics().getNumberOfDocuments()/2);
		tlm_mi.setDirMu(mu);
		tlm_mi.setNumber_of_top_translation_terms(numtopterms);
		//tlm_mi.setRarethreshold(10);
		//tlm_mi.setTopthreshold(index.getCollectionStatistics().getNumberOfDocuments());
		//System.out.println("Translation thresholds: Lower=" + index.getCollectionStatistics().getNumberOfDocuments()/200 + "\t Upper=" + index.getCollectionStatistics().getNumberOfDocuments()/2);
		
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
		
		
		TranslationLMManager tlm_w2v_cbow = new TranslationLMManager(index);
		tlm_w2v_cbow.setTranslation("w2v");
		tlm_w2v_cbow.setRarethreshold(index.getCollectionStatistics().getNumberOfDocuments()/200);
		tlm_w2v_cbow.setTopthreshold(index.getCollectionStatistics().getNumberOfDocuments()/2);
		tlm_w2v_cbow.setDirMu(mu);
		tlm_w2v_cbow.setNumber_of_top_translation_terms(numtopterms);
		//tlm_w2v_cbow.setRarethreshold(10);
		//tlm_w2v_cbow.setTopthreshold(index.getCollectionStatistics().getNumberOfDocuments());
		//System.out.println("Translation thresholds: Lower=" + index.getCollectionStatistics().getNumberOfDocuments()/200 + "\t Upper=" + index.getCollectionStatistics().getNumberOfDocuments()/2);
		System.out.println("Initialise word2vec (cbow) translation");
		//tlm_w2v.initialiseW2V("/Users/zuccong/experiments/sigir2015_nlm/vectors_wsj_skipgram_s200_w5_neg20_hs0_sam1e-4_iter5.txt");
		tlm_w2v_cbow.initialiseW2V_atquerytime(args[5]);
		//tlm_w2v_cbow.initialiseW2V_atquerytime("/Users/zuccong/experiments/sigir2015_nlm/vectors_wsj_cbow_s200_w5_neg20_hs0_sam1e-4_iter5.txt");
		System.out.println("word2vec (cbow) translation initialised");
		
		
		
		/*Properties p = new Properties();
		queryingManager.setProperties(p);*/
		
		/*
		TRECQuery trecqueries = new TRECQuery(new File(args[2]));
		Vector<String> vecStringQueries = new Vector<String>();
		Vector<String> vecStringIds = new Vector<String>();
		trecqueries.extractQuery(args[2], vecStringQueries, vecStringIds);
		*/

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
		
		
		TRECDocnoOutputFormat TRECoutput = new TRECDocnoOutputFormat(index);
		PrintWriter pt = new PrintWriter(new File(args[3]+"_dir.txt"));
		
		TRECDocnoOutputFormat TRECoutput_mi = new TRECDocnoOutputFormat(index);
		PrintWriter pt_mi = new PrintWriter(new File(args[3] + "_dir_mi.txt"));
		
		TRECDocnoOutputFormat TRECoutput_w2v_skipgram = new TRECDocnoOutputFormat(index);
		PrintWriter pt_w2v_skipgram = new PrintWriter(new File(args[3] + "_dir_w2v_skipgram.txt"));
		
		TRECDocnoOutputFormat TRECoutput_w2v_cbow = new TRECDocnoOutputFormat(index);
		PrintWriter pt_w2v_cbow = new PrintWriter(new File(args[3] + "_dir_w2v_cbow.txt"));
		
		//tlm.setProperty("termpipelines", "Stopwords");
		/*
		while(trecqueries.hasNext()) {
			String query = trecqueries.next();
			String qid = trecqueries.getQueryId();
			System.out.println(query + " - " + qid);
			
			Request rq = new Request();
			rq.setOriginalQuery(query);
			rq.setQueryID(qid);
			tlm.runPreProcessing(rq);
			//possibly better off working o request rather than search request
			rq = tlm.runMatching(rq, "null", "dir");
			
			
			TRECoutput.printResults(pt, rq, "dir", "Q0", 1000);
		}*/
		for(String qid : trecqueries.keySet()) {
			String query = trecqueries.get(qid);
			System.out.println(query + " - " + qid);
			
			System.out.println("Scoring with Dir LM");
			//scoring with LM dir
			Request rq = new Request();
			rq.setOriginalQuery(query);
			rq.setQueryID(qid);
			rq = tlm.runMatching(rq, "null", "dir");
			TRECoutput.printResults(pt, rq, "dir", "Q0", 1000);
			
			System.out.println("Scoring with Dir TLM with MI");
			//scoring with LM dir mi
			Request rq_mi = new Request();
			rq_mi.setOriginalQuery(query);
			rq_mi.setQueryID(qid);
			rq_mi = tlm_mi.runMatching(rq_mi, "mi", "dir");
			TRECoutput_mi.printResults(pt_mi, rq_mi, "dir_mi", "Q0", 1000);
			
			System.out.println("Scoring with Dir TLM with w2v (skipgram)");
			//scoring with LM dir w2v
			Request rq_w2v = new Request();
			rq_w2v.setOriginalQuery(query);
			rq_w2v.setQueryID(qid);
			rq_w2v = tlm_w2v_skipgram.runMatching(rq_w2v, "w2v", "dir");
			TRECoutput_w2v_skipgram.printResults(pt_w2v_skipgram, rq_w2v, "dir_w2v_skipgram", "Q0", 1000);
			
			System.out.println("Scoring with Dir TLM with w2v (cbow)");
			//scoring with LM dir w2v
			Request rq_w2v_cbow = new Request();
			rq_w2v_cbow.setOriginalQuery(query);
			rq_w2v_cbow.setQueryID(qid);
			rq_w2v_cbow = tlm_w2v_cbow.runMatching(rq_w2v_cbow, "w2v", "dir");
			TRECoutput_w2v_cbow.printResults(pt_w2v_cbow, rq_w2v_cbow, "dir_w2v_cbow", "Q0", 1000);
			
		}
		pt.flush();
		pt.close();
		pt_mi.flush();
		pt_mi.close();
		pt_w2v_skipgram.flush();
		pt_w2v_skipgram.close();
		pt_w2v_cbow.flush();
		pt_w2v_cbow.close();
		
		/*
		String query = "fracture skull";
		
		//SearchRequest srq = queryingManager.newSearchRequest("queryID0", query);
		SearchRequest srq = tlm.newSearchRequest("queryID0", query);
		srq.addMatchingModel("Matching", "PL2");
		Manager man = new Manager();
		*/
		
		
		
		
		
		/*
		//queryingManager.runPreProcessing(srq);
		//queryingManager.runMatching(srq);
		//queryingManager.runPostProcessing(srq);
		//queryingManager.runPostFilters(srq);
		ResultSet rs = srq.getResultSet();
		
		int[] opDocIds = rs.getDocids();
		double[] opScores = rs.getScores();
		for (int i = 0; i < opDocIds.length; i++) {
			System.out.println(opDocIds[i] +" " + opScores[i]);
		}*/

	}

}
