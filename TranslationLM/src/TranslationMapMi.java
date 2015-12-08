import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.util.Map.Entry;

import org.terrier.matching.models.WeightingModelLibrary;
import org.terrier.structures.DocumentIndex;
import org.terrier.structures.Index;
import org.terrier.structures.Lexicon;
import org.terrier.structures.LexiconEntry;
import org.terrier.structures.PostingIndex;
import org.terrier.structures.postings.IterablePosting;

import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;

/**
 * 
 */

/**
 * @author zuccong
 *
 *	This object implements mutual information translation measures and statistics
 *
 */
public class TranslationMapMi extends TranslationMap implements Serializable{

	public transient CooccurenceMap cooccurencemap=new CooccurenceMap();
	HashMap<String,Double> sumws_u = new HashMap<String,Double>();
	transient Index index = null;
	
	
	
	
	int rarethreshold=0;
	int topthreshold=100000000;
	
	
	/**
	 * Constructor of a Translation Map from an index. This will compute the underlying co-occurrence map
	 * 
	 * @throws IOException 
	 * 
	 */
	public TranslationMapMi(String index_path, String index_prefix) throws IOException {
		super();
		this.cooccurencemap.set_index(index_path, index_prefix);
		this.cooccurencemap.setRarethreshold(500);
		this.cooccurencemap.setTopthreshold(this.cooccurencemap.index.getCollectionStatistics().getNumberOfDocuments()/1000);
		this.cooccurencemap.build_full_cooccurencemap_docversion();
	}
	
	public void set_index(String index_path, String index_prefix) {
		this.index = Index.createIndex(index_path, index_prefix);
		//this.inv = index.getInvertedIndex();
	}
	
	
	/**
	 * Constructor of a Translation Map from an index and a co-occurence file from disk. This will read the underlying co-occurrence map from file
	 * 
	 * @throws IOException 
	 * 
	 */
	public TranslationMapMi(String index_path, String index_prefix, String cooccurencemap_path) throws IOException {
		super();
		
		CooccurenceMap mapInFile = new CooccurenceMap(cooccurencemap_path);
		/*try{
			FileInputStream fis=new FileInputStream(cooccurencemap_path);
			ObjectInputStream ois=new ObjectInputStream(fis);
			System.out.println(ois.toString());
			mapInFile=(CooccurenceMap)ois.readObject();
			
			ois.close();
			fis.close();
		}catch(Exception e){}*/
		//System.out.println(mapInFile.cooccurencemap.size());
		this.cooccurencemap = mapInFile;
		
		//this.cooccurencemap = this.cooccurencemap.readmap(cooccurencemap_path);
		this.cooccurencemap.set_index(index_path, index_prefix);
		
		this.cooccurencemap.setRarethreshold(500);
		this.cooccurencemap.setTopthreshold(this.cooccurencemap.index.getCollectionStatistics().getNumberOfDocuments()/1000);
		
	}
	
	
	/**
	 * Constructor of a Translation Map without an index. This is to be used only for temporary copies
	 * 
	 * @throws IOException 
	 * 
	 */
	public TranslationMapMi() throws IOException {
		super();
		this.translationmap = new HashMap<String, HashMap<String, Double> >();
		this.inverted_translationmap = new HashMap<String, TreeMultimap<Double, String> >();
	}

	
	
	/**
	 * @return the rarethreshold
	 */
	public int getRarethreshold() {
		return rarethreshold;
	}


	/**
	 * @param rarethreshold the rarethreshold to set
	 */
	public void setRarethreshold(int rarethreshold) {
		this.rarethreshold = rarethreshold;
	}


	/**
	 * @return the topthreshold
	 */
	public int getTopthreshold() {
		return topthreshold;
	}


	/**
	 * @param topthreshold the topthreshold to set
	 */
	public void setTopthreshold(int topthreshold) {
		this.topthreshold = topthreshold;
	}


	public void computemi() throws IOException {
		System.out.println("\tcomputing the translation map");
		double N = (double)this.cooccurencemap.index.getCollectionStatistics().getNumberOfDocuments();
		Lexicon<String> lex = this.cooccurencemap.index.getLexicon();
		int count=0;
		
		for (String w : this.cooccurencemap.cooccurencemap.keySet()) {
			if(count % 1000 == 0)
				System.out.println("Processing... " + 100.0*((double)count)/this.cooccurencemap.cooccurencemap.size() + "%");
			count++;
			HashMap<String, Integer> w_cooccurence = this.cooccurencemap.cooccurencemap.get(w);
			LexiconEntry lew = lex.getLexiconEntry(w);
			double p_Xw_1 = (double)lew.getDocumentFrequency()/N;
			double p_Xw_0 = 1.0-p_Xw_1;
			for (String u : w_cooccurence.keySet()) {
				double cooccurence_freq_w_u = (double) w_cooccurence.get(u);
				LexiconEntry leu = lex.getLexiconEntry(u);
				double p_Xu_1 = (double)leu.getDocumentFrequency()/N;
				double p_Xu_0 = 1.0-p_Xu_1;
				double p_Xw_1_Xu_1 = cooccurence_freq_w_u/N; //(double)this.cooccurencemap.get_w_u(w, u)/N; //these are the same, just the first option is cheaper
				double p_Xw_1_Xu_0 = (double)(lew.getDocumentFrequency() - this.cooccurencemap.get_w_u(w, u))/N;
				double p_Xw_0_Xu_1 = (double)(leu.getDocumentFrequency() - this.cooccurencemap.get_w_u(w, u))/N;
				double p_Xw_0_Xu_0 = 1.0 - p_Xw_0_Xu_1 - p_Xw_1_Xu_0 - p_Xw_1_Xu_1;
				
				double I_w_u = p_Xw_0_Xu_0 * WeightingModelLibrary.log( p_Xw_0_Xu_0 / (p_Xw_0 * p_Xu_0) ) 
						+ p_Xw_1_Xu_0 * WeightingModelLibrary.log( p_Xw_1_Xu_0 / (p_Xw_1 * p_Xu_0) )
						+ p_Xw_0_Xu_1 * WeightingModelLibrary.log( p_Xw_0_Xu_1 / (p_Xw_0 * p_Xu_1) )
						+ p_Xw_1_Xu_1 * WeightingModelLibrary.log( p_Xw_1_Xu_1 / (p_Xw_1 * p_Xu_1) )
						;
				
				this.put_p_w_u(w, u, I_w_u);
				double sumws_u =0.0;
				if(this.sumws_u.containsKey(u)) {
					sumws_u = this.sumws_u.get(u);
					this.sumws_u.remove(u);
				}
				sumws_u = sumws_u + I_w_u;
				this.sumws_u.put(u, sumws_u);
			}
			//this should free some space, but it may cause issues on the iteration
			//this.cooccurencemap.cooccurencemap.remove(w);

		}
		System.out.println("\tnormalising the translation map");
		//normalisation step
		TranslationMapMi tmp = new TranslationMapMi();
		for (String w : this.translationmap.keySet()) {
			HashMap<String, Double> w_cooccurence = this.translationmap.get(w);
			for (String u : w_cooccurence.keySet()) {
				if(!this.sumws_u.containsKey(u)) {
					System.err.println("Enquiring the MI table for a term pair that doesn't occur");
					continue;
				}
				double sumws_u = this.sumws_u.get(u);
				double I_w_u = w_cooccurence.get(u);
				tmp.put_p_w_u(w, u, I_w_u/sumws_u);
			}
		}
		//updating the translation map
		System.out.println("\tupdating the translation map");
		this.translationmap = tmp.translationmap;
		System.out.println("\tdone");
	}
	
	/** Serializes the object of type CooccurenceMap into a file
	 * 
	 * @param filepath - the path of the file on disk containing the CooccurenceMap
	 * @param filepath - the CooccurenceMap to serialize
	 */
	public void writemap(String filepath){
		try{
			FileOutputStream fos=new FileOutputStream(filepath);
			ObjectOutputStream oos=new ObjectOutputStream(fos);

			oos.writeObject(this);
			oos.flush();
			oos.close();
			fos.close();
		}catch(Exception e){}
	}
	
	
	public void computemi_fromscratch() throws IOException{
		System.out.println("\tcomputing the co-occurence map");
		//Index index = Index.createIndex(index_path, index_prefix);
		System.out.println("Start size= " + this.translationmap.size());
		this.setRarethreshold(50);
		this.setTopthreshold(index.getCollectionStatistics().getNumberOfDocuments()/2); //if it appears in half of the documents, then it is not considered
		
		PostingIndex di = index.getDirectIndex();
		DocumentIndex doi = index.getDocumentIndex();
		Lexicon<String> lex = index.getLexicon();
		for(int docid=0; docid<doi.getNumberOfDocuments(); docid++) {
			if(docid % 1000 == 0)
				System.out.println("Processing... " + 100.0*((double)docid)/doi.getNumberOfDocuments() + "%");
			IterablePosting postings = di.getPostings(doi.getDocumentEntry(docid));
			Vector<String> seenterms = new Vector<String>();
			while (postings.next() != IterablePosting.EOL) {
				Map.Entry<String,LexiconEntry> lee = lex.getLexiconEntry(postings.getId());
				String termw = lee.getKey();
				//System.out.println("lee.getValue().getFrequency() " + lee.getValue().getFrequency());				
				if(lee.getValue().getFrequency()<this.rarethreshold || lee.getValue().getDocumentFrequency()>this.topthreshold) {
					//System.out.println(termw + " " + lee.getValue().getFrequency() + " < " + this.rarethreshold + " or > " +  this.topthreshold);
					continue;
				}
				//System.out.println("processing term " + termw);
				HashMap<String, Double> w_cooccurence = new HashMap<String, Double>();
				if(this.translationmap.containsKey(termw)) {
					w_cooccurence = this.translationmap.get(termw);
					this.translationmap.remove(termw);
				}
				Iterator<String> it = seenterms.iterator();
			    while(it.hasNext()) {
			    	String termu = it.next();
			    	double count =1.0;
			    	if(w_cooccurence.containsKey(termu)) {
		        		count = count + (double)w_cooccurence.get(termu);
		        		w_cooccurence.remove(termu);
		        	}
					w_cooccurence.put(termu, count);
					//System.out.println("\tputting " + termu + " with count " + count + " for term " + termw);
					
					//System.out.println(termw + ": " + w_cooccurence);
			    	//and now I need to do the symmetric
					HashMap<String, Double> u_cooccurence = new HashMap<String, Double>();
					if(this.translationmap.containsKey(termu)) {
						u_cooccurence = this.translationmap.get(termu);
						this.translationmap.remove(termu);
					}
					double countu=1;
					if(u_cooccurence.containsKey(termw)) {
		        		countu = countu + (double)u_cooccurence.get(termw);
		        		u_cooccurence.remove(termw);
		        	}
					u_cooccurence.put(termw, count);
					this.translationmap.put(termu, u_cooccurence);
					//System.out.println(termu + ": " + u_cooccurence);
			    }
			    
			    this.translationmap.put(termw, w_cooccurence);
			    seenterms.add(termw); // I add only the termw that are within the thresholds
			}
		}
		System.out.println("First pass size= " + this.translationmap.size());
		
		
		System.out.println("\tcomputing the translation map");
		double N = (double)index.getCollectionStatistics().getNumberOfDocuments();
		lex = index.getLexicon();
		int count=0;
		
		for (String w : this.translationmap.keySet()) {
			if(count % 1000 == 0)
				System.out.println("Processing... " + 100.0*((double)count)/this.translationmap.size() + "%");
			count++;
			HashMap<String, Double> w_cooccurence = this.translationmap.get(w);
			LexiconEntry lew = lex.getLexiconEntry(w);
			double p_Xw_1 = (double)lew.getDocumentFrequency()/N;
			double p_Xw_0 = 1.0-p_Xw_1;
			for (String u : w_cooccurence.keySet()) {
				double cooccurence_freq_w_u = (double) w_cooccurence.get(u);
				LexiconEntry leu = lex.getLexiconEntry(u);
				double p_Xu_1 = (double)leu.getDocumentFrequency()/N;
				double p_Xu_0 = 1.0-p_Xu_1;
				double p_Xw_1_Xu_1 = cooccurence_freq_w_u/N; //(double)this.cooccurencemap.get_w_u(w, u)/N; //these are the same, just the first option is cheaper
				double p_Xw_1_Xu_0 = (double)(lew.getDocumentFrequency() - this.translationmap.get(w).get(u))/N;//    .get_w_u(w, u))/N;
				double p_Xw_0_Xu_1 = (double)(leu.getDocumentFrequency() - this.translationmap.get(w).get(u))/N;//    .get_w_u(w, u))/N;
				double p_Xw_0_Xu_0 = 1.0 - p_Xw_0_Xu_1 - p_Xw_1_Xu_0 - p_Xw_1_Xu_1;
				
				double I_w_u = p_Xw_0_Xu_0 * WeightingModelLibrary.log( p_Xw_0_Xu_0 / (p_Xw_0 * p_Xu_0) ) 
						+ p_Xw_1_Xu_0 * WeightingModelLibrary.log( p_Xw_1_Xu_0 / (p_Xw_1 * p_Xu_0) )
						+ p_Xw_0_Xu_1 * WeightingModelLibrary.log( p_Xw_0_Xu_1 / (p_Xw_0 * p_Xu_1) )
						+ p_Xw_1_Xu_1 * WeightingModelLibrary.log( p_Xw_1_Xu_1 / (p_Xw_1 * p_Xu_1) )
						;
				this.translationmap.get(w).put(u, I_w_u);
				
				
				double sumws_u =0.0;
				if(this.sumws_u.containsKey(u)) {
					sumws_u = this.sumws_u.get(u);
					this.sumws_u.remove(u);
				}
				sumws_u = sumws_u + I_w_u;
				this.sumws_u.put(u, sumws_u);
			}


		}
		System.out.println("Second pass size= " + this.translationmap.size());
		System.out.println("\tnormalising the translation map");
		//normalisation step
		for (String w : this.translationmap.keySet()) {
			HashMap<String, Double> w_cooccurence = this.translationmap.get(w);
			TreeMultimap<Double, String> u_inverted_distribution = TreeMultimap.create(Ordering.natural().reverse(), Ordering.natural());
			
			for (String u : w_cooccurence.keySet()) {
				if(!this.sumws_u.containsKey(u)) {
					System.err.println("Enquiring the MI table for a term pair that doesn't occur");
					continue;
				}
				double sumws_u = this.sumws_u.get(u);
				double I_w_u = w_cooccurence.get(u);
				double normalised = I_w_u/sumws_u;
				this.translationmap.get(w).put(u, normalised);
				
				
				//building the inverted map
				u_inverted_distribution.put(normalised, u);
				
				
			}
			this.inverted_translationmap.put(w, u_inverted_distribution);
		}
		System.out.println("\tdone");
		System.out.println("Size after normalisation= " + this.translationmap.size());
		
		
	}
	
	
	public void printTranslationMap(String filename) throws FileNotFoundException {
		PrintWriter writer = new PrintWriter(filename);
		for (String w : this.translationmap.keySet()) {

			writer.print(w);
			HashMap<String, Double> w_translation = this.translationmap.get(w);
			for (String u : w_translation.keySet()) {
				System.out.println(", " + u + " " + w_translation.get(u));
			}
			writer.print("\n");
		}
		writer.flush();
		writer.close();
		
	}
	
	
	public void printInvertedTranslationMap(String filename) throws FileNotFoundException {
		PrintWriter writer = new PrintWriter(filename);
		for (String w : this.inverted_translationmap.keySet()) {

			writer.print(w);
			TreeMultimap<Double, String> w_translation = this.inverted_translationmap.get(w);
			for (Double p_w_u : w_translation.keySet()) {
				System.out.println(", " + w_translation.get(p_w_u) + " " + p_w_u);
			}
			writer.print("\n");
		}
		writer.flush();
		writer.close();
		
	}
	
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		
		/*Version that runs out of the co-occurence map*/
		/*
		//System.setProperty("terrier.home", "/Users/zuccong/tools/terrier-4.0");
		System.setProperty("terrier.home", args[0]);
		
		//TranslationMapMi tmmi = new TranslationMapMi("/Users/zuccong/experiments/dotgov_stoplist/", "data", "/Users/zuccong/experiments/sigir2015_nlm/cooccurence_dotgov_stoplist");
		TranslationMapMi tmmi = new TranslationMapMi(args[1], "data", args[2]);
		
		System.out.println("Size of co-occurence map: " +  tmmi.cooccurencemap.cooccurencemap.size());
		tmmi.computemi();
		System.out.println("tmmi.translationmap.size() = " + tmmi.translationmap.size());
		//tmmi.writemap("/Users/zuccong/experiments/sigir2015_nlm/pmi_dotgov_stoplist");
		tmmi.writemap(args[3]);
		*/
		
		/*Independent Version */
		System.setProperty("terrier.home", args[0]);
		TranslationMapMi tmmi = new TranslationMapMi();
		tmmi.set_index(args[1], "data");
		tmmi.computemi_fromscratch();
		tmmi.writemap(args[2]+".ser");
		tmmi.printTranslationMap(args[2]+"_direct.txt");
		tmmi.printInvertedTranslationMap(args[2]+"_inverted.txt");
		
		
		//This is just a testing loop: will only examine the first 5 terms
		int itcount = 5;
		
		System.out.println(tmmi.translationmap.size());
		
		for (String w : tmmi.translationmap.keySet()) {
			if(itcount>0) {
				itcount--;
				System.out.println(w);
				HashMap<String, Double> w_translation = tmmi.translationmap.get(w);
				for (String u : w_translation.keySet()) {
					System.out.println("\t" + u + ": " + w_translation.get(u));
				}
			}
		}
	}

}
