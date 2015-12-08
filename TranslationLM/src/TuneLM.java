import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Vector;

import org.terrier.querying.Request;
import org.terrier.structures.DocumentIndex;
import org.terrier.structures.DocumentIndexEntry;
import org.terrier.structures.Index;
import org.terrier.structures.MetaIndex;
import org.terrier.structures.outputformat.TRECDocnoOutputFormat;

/**
 * 
 */

/**
 * @author zuccong
 *
 */
public class TuneLM {

	/**
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
		
		System.setProperty("terrier.home", args[0]);
		Index index = Index.createIndex(args[1], "data");
		System.out.println(index.getEnd());
		

		
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
		
		double [ ]  muvalues = { 100.0, 500.0, 1000.0, 1500.0, 2000.0, 2500.0, 3000.0, 3500.0, 4000.0};
		for(int i = 0; i<muvalues.length;i++) {
			double mu = muvalues[i];
			
			TranslationLMManager tlm = new TranslationLMManager(index);
			tlm.setTranslation("null");
			tlm.setDirMu(mu);
			TRECDocnoOutputFormat TRECoutput = new TRECDocnoOutputFormat(index);
			PrintWriter pt = new PrintWriter(new File(args[3]+"_dir_mu_" + String.valueOf(mu) + ".txt"));
			/*
			TranslationLMManager tlm_theory = new TranslationLMManager(index);
			tlm_theory.setTranslation("dir_theory");
			tlm_theory.setDirMu(mu);
			TRECDocnoOutputFormat TRECoutput_theory = new TRECDocnoOutputFormat(index);
			PrintWriter pt_theory = new PrintWriter(new File(args[3]+"_dir_theory_mu_" + String.valueOf(mu) + ".txt"));
			*/
			for(String qid : trecqueries.keySet()) {
				String query = trecqueries.get(qid);
				System.out.println(query + " - " + qid);
				
				System.out.println("Scoring with Dir LM; mu=" + mu);
				//scoring with LM dir
				Request rq = new Request();
				rq.setOriginalQuery(query);
				rq.setIndex(index);
				rq.setQueryID(qid);
				rq = tlm.runMatching(rq, "null", "dir");
				/*
				DocumentIndex doi = index.getDocumentIndex();
				MetaIndex meta = index.getMetaIndex();
				int docid = 1247748; //docids are 0-based
				DocumentIndexEntry die = doi.getDocumentEntry(docid);
				System.out.println(meta.getItem("docno", docid) + ":" + die.getDocumentLength());
				die = doi.getDocumentEntry(docid+1);
				System.out.println(meta.getItem("docno", docid+1) + ":" + die.getDocumentLength());
				meta.
				die = doi.getDocumentEntry(docid+2);
				System.out.println(meta.getItem("docno", docid+2) + ":" + die.getDocumentLength());
				*/
				TRECoutput.printResults(pt, rq, "dir", "Q0", 1000);
				/*
				Request rq_theory = new Request();
				rq_theory.setOriginalQuery(query);
				rq_theory.setIndex(index);
				rq_theory.setQueryID(qid);
				rq_theory = tlm_theory.runMatching(rq_theory, "dir_theory", "dir");
				TRECoutput_theory.printResults(pt_theory, rq_theory, "dir_theory", "Q0", 1000);
				*/
			}
			pt.flush();
			pt.close();
			//pt_theory.flush();
			//pt_theory.close();
		}

	}

}
