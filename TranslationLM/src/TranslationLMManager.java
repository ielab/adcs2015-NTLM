

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;

import org.terrier.matching.CollectionResultSet;
import org.terrier.matching.Matching;
import org.terrier.matching.MatchingQueryTerms;
import org.terrier.matching.QueryResultSet;
import org.terrier.matching.ResultSet;
import org.terrier.matching.models.WeightingModelLibrary;
import org.terrier.querying.Manager;
import org.terrier.querying.Request;
import org.terrier.querying.SearchRequest;
import org.terrier.querying.parser.Query;
import org.terrier.structures.BitIndexPointer;
import org.terrier.structures.DocumentIndex;
import org.terrier.structures.DocumentIndexEntry;
import org.terrier.structures.Index;
import org.terrier.structures.Lexicon;
import org.terrier.structures.LexiconEntry;
import org.terrier.structures.MetaIndex;
import org.terrier.structures.PostingIndex;
import org.terrier.structures.bit.DirectIndex;
import org.terrier.structures.bit.InvertedIndex;
import org.terrier.structures.postings.IterablePosting;
import org.terrier.terms.TermPipelineAccessor;

import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;

/**
 * @author zuccong
 *
 */
public class TranslationLMManager extends Manager{

	public ResultSet rs = new CollectionResultSet(index.getEnd());
	private String[] queryTerms=null;
	private PostingIndex invertedIndex = this.index.getInvertedIndex();
	public double mu=2500.0;
	private Lexicon lex = index.getLexicon();
	public double alpha=0.7;
	
	public HashMap<String, TreeMultimap<Double, String> > w2v_inverted_translation = new HashMap<String, TreeMultimap<Double, String> >();
	
	public TranslationMap translationmap = null;
	public String translation_method = "null";
	public int number_of_top_translation_terms=10; //default value
	int rarethreshold=0;
	int topthreshold=100000000;

	public String index_path="";
	public String index_prefix=""; 
	public String cooccurencemap_path="";
	
	HashMap<String, double[]> fullw2vmatrix = new HashMap<String, double[]>();
	
	String path_to_mi_window_folder="";
	int windowsize=0;
	HashMap<String, HashMap<String, Double>> window_translations = new HashMap<String, HashMap<String, Double>>();
	
	/**
	 * @return the windowsize
	 */
	public int getWindowsize() {
		return windowsize;
	}


	/**
	 * @param windowsize the windowsize to set
	 */
	public void setWindowsize(int windowsize) {
		this.windowsize = windowsize;
	}


	/**
	 * @return the path_to_mi_window_folder
	 */
	public String getPath_to_mi_window_folder() {
		return path_to_mi_window_folder;
	}


	/**
	 * @param path_to_mi_window_folder the path_to_mi_window_folder to set
	 */
	public void setPath_to_mi_window_folder(String path_to_mi_window_folder) {
		this.path_to_mi_window_folder = path_to_mi_window_folder;
	}


	public TranslationLMManager(Index index) {
		super(index);
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
	 * @return the index_path
	 */
	public String getIndex_path() {
		return index_path;
	}

	/**
	 * @param index_path the index_path to set
	 */
	public void setIndex_path(String index_path) {
		this.index_path = index_path;
	}

	/**
	 * @return the index_prefix
	 */
	public String getIndex_prefix() {
		return index_prefix;
	}

	/**
	 * @param index_prefix the index_prefix to set
	 */
	public void setIndex_prefix(String index_prefix) {
		this.index_prefix = index_prefix;
	}

	/**
	 * @return the cooccurencemap_path
	 */
	public String getCooccurencemap_path() {
		return cooccurencemap_path;
	}

	/**
	 * @param cooccurencemap_path the cooccurencemap_path to set
	 */
	public void setCooccurencemap_path(String cooccurencemap_path) {
		this.cooccurencemap_path = cooccurencemap_path;
	}

	
	/**
	 * @return the number_of_top_translation_terms
	 */
	public int getNumber_of_top_translation_terms() {
		return number_of_top_translation_terms;
	}


	/**
	 * @param number_of_top_translation_terms the number_of_top_translation_terms to set
	 */
	public void setNumber_of_top_translation_terms(
			int number_of_top_translation_terms) {
		this.number_of_top_translation_terms = number_of_top_translation_terms;
	}


	/**
	 * @return the translation method
	 */
	public String getTranslation() {
		return translation_method;
	}

	/**
	 * @param translation the translation method to set
	 */
	public void setTranslation(String translation) {
		this.translation_method = translation;
	}
	
	/**
	 * @return the alpha
	 */
	public double getAlpha() {
		return alpha;
	}


	/**
	 * @param alpha the alpha to set
	 */
	public void setAlpha(double alpha) {
		this.alpha = alpha;
	}


	public void initialiseTranslation() throws IOException {
		switch (this.translation_method.toLowerCase()) {
        case "null":
        	//no translation selected
        	System.err.println("No translation method set, but trying to initialise a translation");
            break;
        case "mi":
        	//want to use Mutual Information for translation
        	translationmap = new TranslationMapMi(this.index_path, this.index_prefix, this.cooccurencemap_path);
            break;
        case "w2v":
        	//want to use Mutual Information for translation
        	translationmap = new TranslationMapW2v();
            break;
        default: 
        	translationmap = new TranslationMapMi(this.index_path, this.index_prefix, this.cooccurencemap_path);
            break;
		}
	}
	
	/** Reads a serialized object of type HashMap<String, TreeMultimap<Double, String> > (w2v serialised inverted map)
	 * 
	 * @param filepath - the path of the file on disk containing the w2v serialised object
	 */
	public void readW2VSerialised(String filepath){
		try{
			FileInputStream fis=new FileInputStream(filepath);
			ObjectInputStream ois=new ObjectInputStream(fis);
			this.w2v_inverted_translation=(HashMap<String, TreeMultimap<Double, String> >)ois.readObject();
			ois.close();
			fis.close();
		}catch(Exception e){}
		return;
	}
	
	/** Serializes the object of type HashMap<String, TreeMultimap<Double, String> > (w2v serialised inverted map) to a file
	 * 
	 * @param filepath - the path of the file on disk containing the w2v serialised inverted map
	 */
	public void writemap(String filepath){
		try{
			FileOutputStream fos=new FileOutputStream(filepath);
			ObjectOutputStream oos=new ObjectOutputStream(fos);
			oos.writeObject(this.w2v_inverted_translation);
			oos.flush();
			oos.close();
			fos.close();
		}catch(Exception e){}
	}
	
	public void initialiseW2V(String filepath) throws NumberFormatException, IOException {
		File f = new File(filepath+".ser");
		if(f.exists()) { 
			/* load the matrix that has been serialised to disk */ 
			System.out.println("Loading translations from file");
			this.readW2VSerialised(f.getAbsolutePath());
		}else {
			HashMap<String, double[]> w2vmatrix = new HashMap<String, double[]>();
			BufferedReader br = new BufferedReader(new FileReader(filepath));
			String line = null;
			int count=0;
			int numberofdimensions=0;
			int foundterms=0;
			while ((line = br.readLine()) != null) {
				if(count==0) {
					//this is the first line: it says how many words and how many dimensions
					String[] input = line.split(" ");
					numberofdimensions = Integer.parseInt(input[1]);
					count++;
					continue;
				}
				
				
				String[] input = line.split(" ");
				String term = input[0];
				LexiconEntry lEntry = this.lex.getLexiconEntry(term);
				//screen the term for out of vacabulary and not in the threshold range
				if (lEntry==null)
				{
					//System.err.println("W2V Term Not Found: "+term);
					continue;
				}
				if(lEntry.getFrequency()<this.rarethreshold || lEntry.getDocumentFrequency()<this.rarethreshold 
						|| lEntry.getDocumentFrequency()>this.topthreshold || term.matches(".*\\d+.*"))
					continue;
				
				foundterms++;
				int dimension=0;
				double[] vector = new double[numberofdimensions];
				for(int i=1; i<input.length;i++) {
					vector[dimension] = Double.parseDouble(input[i]);
					dimension++;
				}
				w2vmatrix.put(term, vector);
				count++;
			}
			System.out.println("Terms founds in word2vec: " + foundterms);
			br.close();
			
			//HashMap<String, TreeMultimap<Double, String> > inverted_translation = new HashMap<String, TreeMultimap<Double, String> >();
			for(String w : w2vmatrix.keySet()) {
				double[] vector_w = w2vmatrix.get(w);
				TreeMultimap<Double, String> inverted_translation_w = TreeMultimap.create(Ordering.natural().reverse(), Ordering.natural());
				HashMap<String,Double> tmp_w = new HashMap<String,Double>();
				double sum_cosines=0.0;
				for(String u : w2vmatrix.keySet()) {
					double[] vector_u = w2vmatrix.get(u);
					double cosine_w_u=0.0;
					double sum_w=0.0;
					double sum_u=0.0;
					for(int i=0; i<vector_w.length;i++) {
						cosine_w_u=cosine_w_u + vector_w[i]*vector_u[i];
						sum_w=sum_w + Math.pow(vector_w[i],2);
						sum_u=sum_u + Math.pow(vector_u[i],2);
					}
					//System.out.println("Un-normalised cosine: " + cosine_w_u);
					//normalisation step
					cosine_w_u = cosine_w_u / (Math.sqrt(sum_w) * Math.sqrt(sum_u));
					//System.out.println("normalised cosine: " + cosine_w_u);
					tmp_w.put(u, cosine_w_u);
					sum_cosines = sum_cosines+ cosine_w_u;
				}
				//normalise to probabilities and insert in order
				for(String u: tmp_w.keySet()) {
					double p_w2v_w_u = tmp_w.get(u)/sum_cosines;
					inverted_translation_w.put(p_w2v_w_u, u);
				}
				w2v_inverted_translation.put(w, inverted_translation_w);
			}
			this.writemap(f.getAbsolutePath());
		}
		System.out.println("Initialisation of word2vec finished");
	}
	
	
	public void initialiseW2V_atquerytime(String filepath) throws NumberFormatException, IOException {
		File f = new File(filepath+"_matrix.ser");
		if(f.exists()) { 
			/* load the matrix that has been serialised to disk */ 
			System.out.println("Loading matrix from file");
			//this.readW2VSerialised(f.getAbsolutePath());
			//TODO: to replace with appropriate method
		}else {
			HashMap<String, double[]> w2vmatrix = new HashMap<String, double[]>();
			BufferedReader br = new BufferedReader(new FileReader(filepath));
			String line = null;
			int count=0;
			int numberofdimensions=0;
			int foundterms=0;
			while ((line = br.readLine()) != null) {
				if(count==0) {
					//this is the first line: it says how many words and how many dimensions
					String[] input = line.split(" ");
					numberofdimensions = Integer.parseInt(input[1]);
					count++;
					continue;
				}
				
				
				String[] input = line.split(" ");
				String term = input[0];
				LexiconEntry lEntry = this.lex.getLexiconEntry(term);
				//screen the term for out of vacabulary and not in the threshold range
				if (lEntry==null)
				{
					//System.err.println("W2V Term Not Found: "+term);
					continue;
				}
				if(lEntry.getFrequency()<this.rarethreshold || lEntry.getDocumentFrequency()<this.rarethreshold 
						|| lEntry.getDocumentFrequency()>this.topthreshold || term.matches(".*\\d+.*"))
					continue;
				
				foundterms++;
				int dimension=0;
				double[] vector = new double[numberofdimensions];
				for(int i=1; i<input.length;i++) {
					vector[dimension] = Double.parseDouble(input[i]);
					dimension++;
				}
				w2vmatrix.put(term, vector);
				count++;
			}
			System.out.println("Terms founds in word2vec: " + foundterms);
			br.close();
			this.fullw2vmatrix = w2vmatrix;
			
		}
		System.out.println("Initialisation of word2vec finished");
	}
	
	
	
/*	
	public void runMatching(SearchRequest srq, Matching matching) {
		Request rq = (Request) srq;
		MatchingQueryTerms matchingTerms = rq.getMatchingQueryTerms();
//		System.out.println(rq.getQueryID());
//		for(String term: matchingTerms.getTerms()){
//			System.out.printf(" %s", term);
//		}
		matching.match(rq.getQueryID(), matchingTerms);
		ResultSet outRs = 
				matching.getResultSet();
		rq.setResultSet((ResultSet) (outRs.getResultSet(0, outRs
				.getResultSize())));

	}
*/	

	/** Returns the value of the mu parameter of a Dirichlet smoothing language model
	 */
	public double getMu() {
		return mu;
	}
	
	/** Computes the maximum likelihood probability of a term in a document (these are encoded in the posting ip)
	 * @param ip - the IterablePosting which refers to the term and the document
	 */
	public double p_ml(IterablePosting ip) {
		double p_ml = (double)ip.getFrequency()/ip.getDocumentLength();
		return p_ml;
	}
	
	/** Sets the mu parameter of a Dirichlet smoothing language model
	 * @param valueOfMu - the value of the mu paramter
	 */
	public void setDirMu(double valueOfMu) {
		this.mu=valueOfMu;
	}
	
	/** Sets the mu parameter of a Dirichlet smoothing language model
	 * @param valueOfMu - the value of the mu paramter
	 */
	public void setMu(double valueOfMu) {
		this.mu=valueOfMu;
	}
	
	public LexiconEntry getLexiconEntry(String term) {
		LexiconEntry lEntry = this.lex.getLexiconEntry(term);
		return lEntry;
	}
	
	/** Provides a translation model using mutual information. This method returns the top T (most probable) translations for term w
	 * @param w - the term we want to translate
	 * @return HashMap<String, Double> containing the top T (this.number_of_top_translations) translations for the input term w.
	 */
	public HashMap<String, Double> translate_mi(String w){
		HashMap<String, Double> us = this.translationmap.get_topu_givenw(w, this.number_of_top_translation_terms);
		return us;
	}
	
	
	/** Provides a translation model using skipgram word embeddings (word2vec, w2v). This method returns the top T (most probable) translations for term w
	 * @param w - the term we want to translate
	 * @return HashMap<String, Double> containing the top T (this.number_of_top_translations) translations for the input term w.
	 */
	public HashMap<String, Double> translate_w2v(String w){
		HashMap<String, Double> us = this.translationmap.get_topu_givenw(w, this.number_of_top_translation_terms);
		return us;
	}
	
	
	public HashMap<String, Double> translate(String w){
		HashMap<String, Double> us = new HashMap<String, Double>();
		switch (this.translation_method.toLowerCase()) {
        case "null":
        	//no translation selected
        	System.err.println("No translation method set, but trying to initialise a translation");
            break;
        case "mi":
        	//want to use Mutual Information for translation
        	us = translate_mi(w);
            break;
        default: 
        	//TODO: decide what goes as default. For now returns 0.5
        	us.put("translation", 0.5);
            break;
		}
		return us;
	}
	
	
	

	/** Performs retrieval with Dirichlet smoothing language model. Results are saved in the ResultSet of the object
	 * 
	 * @throws IOException if a problem occurs during matching
	 */
	public void dir() throws IOException {
		MetaIndex meta = index.getMetaIndex();
		System.out.println("Number of docs in the index = " + this.index.getCollectionStatistics().getNumberOfDocuments());
		double[] log_p_d_q = new double[this.index.getCollectionStatistics().getNumberOfDocuments()];

		//iterating over all query terms
		for(int i=0; i<this.queryTerms.length;i++) {
			System.out.println(this.queryTerms[i]);
			LexiconEntry lEntry = this.lex.getLexiconEntry(this.queryTerms[i]);
			if (lEntry==null)
			{
				System.err.println("Term Not Found: "+this.queryTerms[i]);
				continue;
			}
			double p_w_C = (double)lEntry.getFrequency()/this.index.getCollectionStatistics().getNumberOfTokens();
			IterablePosting ip = this.invertedIndex.getPostings(lEntry);

			//iterating over all documents in the posting of this query term


			/*Note this only does best match style of Dirichlet
			 * while this is good for single term queries, it is incorrect for multi term queries
			 * */
			while(ip.next() != IterablePosting.EOL) {
				{

					double tf = (double)ip.getFrequency();
					double c = this.mu;
					double numberOfTokens = (double) this.index.getCollectionStatistics().getNumberOfTokens();
					double docLength = (double) ip.getDocumentLength();
					double colltermFrequency = (double)lEntry.getFrequency();

					double score =	
							WeightingModelLibrary.log( (docLength* tf/docLength + c * (colltermFrequency/numberOfTokens)) / (c + docLength)) 
							- WeightingModelLibrary.log( c/( c+ docLength) * (colltermFrequency/numberOfTokens) ) 
							+ WeightingModelLibrary.log(c/(c + docLength))
							;

					/*tring docno = meta.getItem("docno", ip.getId());
					System.out.println(docno + "\t->\t" + score);
					System.out.println("log_p_d_q[ip.getId()]=" + log_p_d_q[ip.getId()] + "\t" + 
							"docLen=" + docLength + "\ttf=" + tf
							+ "\tcf=" + colltermFrequency + "\tN=" + numberOfTokens + "\tmu="+this.mu
							);*/

					log_p_d_q[ip.getId()] = log_p_d_q[ip.getId()] +  score;

				}
			}
		}
		//now need to put the scores into the result set
		this.rs.initialise(log_p_d_q);
		System.out.println("this.rs.getResultSize()=" + this.rs.getResultSize());
	}
	
	
	/** Performs retrieval with Dirichlet smoothing language model. Results are saved in the ResultSet of the object. This method follows
	 * the theory, where n log mu/(|d| + mu) is done
	 * 
	 * @throws IOException if a problem occurs during matching
	 */
	public void dir_theory() throws IOException {
		MetaIndex meta = index.getMetaIndex();
		double[] log_p_d_q = new double[this.index.getCollectionStatistics().getNumberOfDocuments()];
		Vector<Integer> seendocs = new Vector<Integer>();
		//iterating over all query terms
		for(int i=0; i<this.queryTerms.length;i++) {
			System.out.println(this.queryTerms[i]);
			LexiconEntry lEntry = this.lex.getLexiconEntry(this.queryTerms[i]);
			if (lEntry==null)
			{
				System.err.println("Term Not Found: "+this.queryTerms[i]);
				continue;
			}
			double p_w_C = (double)lEntry.getFrequency()/this.index.getCollectionStatistics().getNumberOfTokens();
			IterablePosting ip = this.invertedIndex.getPostings(lEntry);

			//iterating over all documents in the posting of this query term


			/*Note this only does best match style of Dirichlet
			 * while this is good for single term queries, it is incorrect for multi term queries
			 * */
			while(ip.next() != IterablePosting.EOL) {
				{

					double tf = (double)ip.getFrequency();
					double c = this.mu;
					double numberOfTokens = (double) this.index.getCollectionStatistics().getNumberOfTokens();
					double docLength = (double) ip.getDocumentLength();
					double colltermFrequency = (double)lEntry.getFrequency();

					double score =	
							WeightingModelLibrary.log( (docLength* tf/docLength + c * (colltermFrequency/numberOfTokens)) / (c + docLength)) 
							- WeightingModelLibrary.log( c/( c+ docLength) * (colltermFrequency/numberOfTokens) ) 
							;

					/*tring docno = meta.getItem("docno", ip.getId());
					System.out.println(docno + "\t->\t" + score);
					System.out.println("log_p_d_q[ip.getId()]=" + log_p_d_q[ip.getId()] + "\t" + 
							"docLen=" + docLength + "\ttf=" + tf
							+ "\tcf=" + colltermFrequency + "\tN=" + numberOfTokens + "\tmu="+this.mu
							);*/

					log_p_d_q[ip.getId()] = log_p_d_q[ip.getId()] +  score;
					if(!seendocs.contains(ip.getId()))
						seendocs.add(ip.getId());

				}
			}
		}
		//now add the n log mu/(mu+d) component
		Iterator<Integer> it = seendocs.iterator();
		while(it.hasNext()) {
			int docid = it.next();
			DocumentIndex doi = index.getDocumentIndex();
			DocumentIndexEntry die = doi.getDocumentEntry(docid);
			log_p_d_q[docid] = log_p_d_q[docid] + (double)queryTerms.length * WeightingModelLibrary.log(this.mu/(this.mu + die.getDocumentLength()));
		}
		
		
		//now need to put the scores into the result set
		this.rs.initialise(log_p_d_q);
	}

	
	/** Performs retrieval with a Dirichlet smoothing translation language model, where the translation probability is estimated using mutual information, 
	 * as described in Karimzadehgan, Zhai, "Estimation of Statistical Translation Models Based on Mutual Information for Ad Hoc Information Retrieval", SIGIR 2010
	 * 
	 * Results are saved in the ResultSet of the object
	 * 
	 * @throws IOException if a problem occurs during matching
	 */
	public void dir_t_mi() throws IOException {
		double[] log_p_d_q = new double[this.index.getCollectionStatistics().getNumberOfDocuments()];
		double[] ssum_p_ud_wu = new double[this.index.getCollectionStatistics().getNumberOfDocuments()];
		//iterating over all query terms
		for(int i=0; i<this.queryTerms.length;i++) {
			LexiconEntry lEntry = this.lex.getLexiconEntry(this.queryTerms[i]);
			if (lEntry==null)
			{
				System.err.println("Term Not Found: "+this.queryTerms[i]);
				continue;
			}
			double p_w_C = (double)lEntry.getFrequency()/this.index.getCollectionStatistics().getNumberOfTokens();
			
			//now need to compute the possible translations of w
			HashMap<String, Double> translations_w = translate(this.queryTerms[i]);
			//now iterate through the translations u of w
			for (Map.Entry<String, Double> entry : translations_w.entrySet()) {
			    String u = entry.getKey();
			    Double p_w_u = entry.getValue();
			    //now compute p_u_d
			    LexiconEntry uEntry = this.lex.getLexiconEntry(u);
				if (lEntry==null)
				{
					System.err.println("Term Not Found: "+u);
					continue;
				}
				IterablePosting ip = this.invertedIndex.getPostings(uEntry);
				
				//iterating over all documents in the posting of this translation term and score the document
				while(ip.next() != IterablePosting.EOL) {
					ssum_p_ud_wu[ip.getId()] = ssum_p_ud_wu[ip.getId()] + p_ml(ip) * p_w_u * ((double)ip.getDocumentLength()/(ip.getDocumentLength()+this.mu));
							
				}
				
			}

			//now compute the final score for this query term, including the translation probabilities
			IterablePosting ip = this.invertedIndex.getPostings(lEntry);
			
			//iterating over all documents in the posting of this query term
			while(ip.next() != IterablePosting.EOL) {
				log_p_d_q[ip.getId()] = log_p_d_q[ip.getId()] 
						+ WeightingModelLibrary.log( ssum_p_ud_wu[ip.getId()] // the minus in front of the log is to turn it into a positive score for sorting reasons
								+ this.mu/(this.mu+(double)ip.getDocumentLength()) * p_w_C); 
			}
		}
		//now need to put the scores into the result set
		this.rs.initialise(log_p_d_q);
	}
	
	
	
	/** Performs retrieval with a Dirichlet smoothing translation language model, where the translation probability is estimated using mutual information, 
	 * as described in Karimzadehgan, Zhai, "Estimation of Statistical Translation Models Based on Mutual Information for Ad Hoc Information Retrieval", SIGIR 2010
	 * 
	 * This version performs the translation at query time
	 * 
	 * Results are saved in the ResultSet of the object
	 * 
	 * @throws IOException if a problem occurs during matching
	 * @throws InterruptedException 
	 */
	public void dir_t_mi_atquery() throws IOException, InterruptedException {
		MetaIndex meta = index.getMetaIndex();
		double[] log_p_d_q = new double[this.index.getCollectionStatistics().getNumberOfDocuments()];
		Arrays.fill(log_p_d_q, -1000.0);
		//iterating over all query terms
		for(int i=0; i<this.queryTerms.length;i++) {
			System.out.println(this.queryTerms[i]);
			String w = this.queryTerms[i];
			LexiconEntry lEntry = this.lex.getLexiconEntry(w);
			if (lEntry==null)
			{
				System.err.println("Term Not Found: "+this.queryTerms[i]);
				continue;
			}
			IterablePosting ip = this.invertedIndex.getPostings(lEntry);
	
			System.out.println("\t Obtaining translations for " + w);
			HashMap<String, Double> top_translations_of_w = getTopMiTranslations_unormalisation(w); // <- this is the line to change if we want other transltions
			System.out.println("\t Translations for " + w + " acquired");
			
			/*Preload the probabilities p(u|d)*/
			HashMap<String, HashMap<Integer, Double>> p_u_d_distributions = new HashMap<String, HashMap<Integer, Double>>();
			for(String u : top_translations_of_w.keySet()) {
				Index index = Index.createIndex();
				LexiconEntry lu = this.lex.getLexiconEntry( u );
				IterablePosting postings = this.invertedIndex.getPostings((BitIndexPointer) lu);
				HashMap<Integer, Double> u_distribution = new HashMap<Integer, Double>();
				while (postings.next() != IterablePosting.EOL) {
					int doc = postings.getId();
					double p_u_d = (double)postings.getFrequency()/(double)postings.getDocumentLength();
					u_distribution.put(doc, p_u_d);
				}
				p_u_d_distributions.put(u, u_distribution);
			}
			
			
			
			/*Iterate over all the docs that contain w (iterating over all documents in the posting of this query term)
			 * 
			 * Note this only does best match style of Dirichlet
			 * while this is good for single term queries, it is incorrect for multi term queries
			 * */
			while(ip.next() != IterablePosting.EOL) {
				{
					double tf = (double)ip.getFrequency();
					double c = this.mu;
					double numberOfTokens = (double) this.index.getCollectionStatistics().getNumberOfTokens();
					double docLength = (double) ip.getDocumentLength();
					double colltermFrequency = (double)lEntry.getFrequency();
					
					double sum_p_t_w_u = 0.0;
					for(String u : top_translations_of_w.keySet()) {
						double p_t_w_u = top_translations_of_w.get(u);
						//System.out.println("Translation to " + u + " -> " + p_t_w_u);
						/*double p_u_d=0.0;
						PostingIndex di = index.getDirectIndex();
						DocumentIndex doi = index.getDocumentIndex();
						IterablePosting postings = di.getPostings((BitIndexPointer)doi.getDocumentEntry(ip.getId()));
						while (postings.next() != IterablePosting.EOL) {
							Map.Entry<String,LexiconEntry> lee = lex.getLexiconEntry(postings.getId());
							if(lee.getKey().equalsIgnoreCase(u)) {
								p_u_d = (double)postings.getFrequency()/docLength;
								break;
							}
						}*/
						double p_u_d=0.0;
						if(p_u_d_distributions.get(u).containsKey(ip.getId()))
							p_u_d= p_u_d_distributions.get(u).get(ip.getId());
						sum_p_t_w_u = sum_p_t_w_u + p_t_w_u * p_u_d;
						//System.out.println("Prob u in doc " + p_u_d);
					}
					
					//System.out.println("sum_p_t_w_u=" + sum_p_t_w_u);
					double score =	
							WeightingModelLibrary.log( (docLength* sum_p_t_w_u + c * (colltermFrequency/numberOfTokens)) / (c + docLength)) 
							- WeightingModelLibrary.log( c/( c+ docLength) * (colltermFrequency/numberOfTokens) ) 
							+ WeightingModelLibrary.log(c/(c + docLength))
							;

					/*tring docno = meta.getItem("docno", ip.getId());
					System.out.println(docno + "\t->\t" + score);
					System.out.println("log_p_d_q[ip.getId()]=" + log_p_d_q[ip.getId()] + "\t" + 
							"docLen=" + docLength + "\ttf=" + tf
							+ "\tcf=" + colltermFrequency + "\tN=" + numberOfTokens + "\tmu="+this.mu
							);*/

					if(log_p_d_q[ip.getId()]==-1000.0)
						log_p_d_q[ip.getId()]=0.0;
					
					log_p_d_q[ip.getId()] = log_p_d_q[ip.getId()] +  score;

				}
			}
		}
		//now need to put the scores into the result set
		this.rs.initialise(log_p_d_q);
	}
	
	/** Performs retrieval with a Dirichlet smoothing translation language model, where the translation probability is estimated using mutual information, 
	 * as described in Karimzadehgan, Zhai, "Estimation of Statistical Translation Models Based on Mutual Information for Ad Hoc Information Retrieval", SIGIR 2010
	 * 
	 * This version performs the translation at query time
	 * 
	 * Results are saved in the ResultSet of the object
	 * 
	 * @throws IOException if a problem occurs during matching
	 * @throws InterruptedException 
	 */
	public void dir_t_mi_atquery_self() throws IOException, InterruptedException {
		MetaIndex meta = index.getMetaIndex();
		double[] log_p_d_q = new double[this.index.getCollectionStatistics().getNumberOfDocuments()];
		Arrays.fill(log_p_d_q, -1000.0);
		//iterating over all query terms
		for(int i=0; i<this.queryTerms.length;i++) {
			System.out.println(this.queryTerms[i]);
			String w = this.queryTerms[i];
			LexiconEntry lEntry = this.lex.getLexiconEntry(w);
			if (lEntry==null)
			{
				System.err.println("Term Not Found: "+this.queryTerms[i]);
				continue;
			}
			IterablePosting ip = this.invertedIndex.getPostings(lEntry);
	
			System.out.println("\t Obtaining translations for " + w);
			HashMap<String, Double> top_translations_of_w = getTopMiTranslations_unormalisation(w); // <- this is the line to change if we want other transltions
			System.out.println("\t Translations for " + w + " acquired");
			
			/*Preload the probabilities p(u|d)*/
			HashMap<String, HashMap<Integer, Double>> p_u_d_distributions = new HashMap<String, HashMap<Integer, Double>>();
			for(String u : top_translations_of_w.keySet()) {
				Index index = Index.createIndex();
				LexiconEntry lu = this.lex.getLexiconEntry( u );
				IterablePosting postings = this.invertedIndex.getPostings((BitIndexPointer) lu);
				HashMap<Integer, Double> u_distribution = new HashMap<Integer, Double>();
				while (postings.next() != IterablePosting.EOL) {
					int doc = postings.getId();
					double p_u_d = (double)postings.getFrequency()/(double)postings.getDocumentLength();
					u_distribution.put(doc, p_u_d);
				}
				p_u_d_distributions.put(u, u_distribution);
			}
			
			
			
			/*Iterate over all the docs that contain w (iterating over all documents in the posting of this query term)
			 * 
			 * Note this only does best match style of Dirichlet
			 * while this is good for single term queries, it is incorrect for multi term queries
			 * */
			while(ip.next() != IterablePosting.EOL) {
				{
					double tf = (double)ip.getFrequency();
					double c = this.mu;
					double numberOfTokens = (double) this.index.getCollectionStatistics().getNumberOfTokens();
					double docLength = (double) ip.getDocumentLength();
					double colltermFrequency = (double)lEntry.getFrequency();
					
					double sum_p_t_w_u = 0.0;
					for(String u : top_translations_of_w.keySet()) {
						double p_t_w_u = top_translations_of_w.get(u);
						//System.out.println("Translation to " + u + " -> " + p_t_w_u);
						/*double p_u_d=0.0;
						PostingIndex di = index.getDirectIndex();
						DocumentIndex doi = index.getDocumentIndex();
						IterablePosting postings = di.getPostings((BitIndexPointer)doi.getDocumentEntry(ip.getId()));
						while (postings.next() != IterablePosting.EOL) {
							Map.Entry<String,LexiconEntry> lee = lex.getLexiconEntry(postings.getId());
							if(lee.getKey().equalsIgnoreCase(u)) {
								p_u_d = (double)postings.getFrequency()/docLength;
								break;
							}
						}*/
						double p_u_d=0.0;
						if(p_u_d_distributions.get(u).containsKey(ip.getId()))
							p_u_d= p_u_d_distributions.get(u).get(ip.getId());
						
						//set self translation to 1
						if(u.equals(w))
							p_t_w_u=1;
						
						sum_p_t_w_u = sum_p_t_w_u + p_t_w_u * p_u_d;
						//System.out.println("Prob u in doc " + p_u_d);
					}
					
					//System.out.println("sum_p_t_w_u=" + sum_p_t_w_u);
					double score =	
							WeightingModelLibrary.log( (docLength* sum_p_t_w_u + c * (colltermFrequency/numberOfTokens)) / (c + docLength)) 
							- WeightingModelLibrary.log( c/( c+ docLength) * (colltermFrequency/numberOfTokens) ) 
							+ WeightingModelLibrary.log(c/(c + docLength))
							;

					/*tring docno = meta.getItem("docno", ip.getId());
					System.out.println(docno + "\t->\t" + score);
					System.out.println("log_p_d_q[ip.getId()]=" + log_p_d_q[ip.getId()] + "\t" + 
							"docLen=" + docLength + "\ttf=" + tf
							+ "\tcf=" + colltermFrequency + "\tN=" + numberOfTokens + "\tmu="+this.mu
							);*/

					if(log_p_d_q[ip.getId()]==-1000.0)
						log_p_d_q[ip.getId()]=0.0;
					
					log_p_d_q[ip.getId()] = log_p_d_q[ip.getId()] +  score;

				}
			}
		}
		//now need to put the scores into the result set
		this.rs.initialise(log_p_d_q);
	}
	
	
	/** Performs retrieval with a Dirichlet smoothing translation language model, where the translation probability is estimated using mutual information, 
	 * as described in Karimzadehgan, Zhai, "Estimation of Statistical Translation Models Based on Mutual Information for Ad Hoc Information Retrieval", SIGIR 2010
	 * 
	 * This version performs the translation at query time
	 * 
	 * Results are saved in the ResultSet of the object
	 * 
	 * @throws IOException if a problem occurs during matching
	 * @throws InterruptedException 
	 */
	public void dir_t_mi_atquery_withalpha() throws IOException, InterruptedException {
		double[] log_p_d_q = new double[this.index.getCollectionStatistics().getNumberOfDocuments()];
		Arrays.fill(log_p_d_q, -1000.0);

		PostingIndex<?> di = index.getDirectIndex();
		DocumentIndex doi = index.getDocumentIndex();
		
		double c = this.mu;
		double numberOfTokens = (double) this.index.getCollectionStatistics().getNumberOfTokens();
		
		//iterating over all query terms
		for(int i=0; i<this.queryTerms.length;i++) {
			System.out.println(this.queryTerms[i]);
			String w = this.queryTerms[i];
			LexiconEntry lEntry = this.lex.getLexiconEntry(w);
			if (lEntry==null)
			{
				System.err.println("Term Not Found: "+this.queryTerms[i]);
				continue;
			}
			//IterablePosting ip = this.invertedIndex.getPostings(lEntry);
	
			System.out.println("\t Obtaining translations for " + w + "\t(cf=" + lEntry.getFrequency() + "; docf=" + lEntry.getDocumentFrequency());
			if(lEntry.getFrequency()<this.rarethreshold || lEntry.getDocumentFrequency()<this.rarethreshold 
					|| lEntry.getDocumentFrequency()>this.topthreshold || w.matches(".*\\d+.*"))
				System.out.println("Term " + w + " matches the conditions for not beeing considered by w2v (cf, docf<" + this.rarethreshold + " || docf>" + this.topthreshold+ ")");
			
			
			HashMap<String, Double> top_translations_of_w = getTopMiTranslations_unormalisation_alpha(w);
			System.out.println("\t" + top_translations_of_w.size() + " Translations for " + w + " acquired");
			
			
			if(!top_translations_of_w.containsKey(w)) {
				System.err.println("Attention, there is no self-translation for term " + w);
				//setting the self-translation to s
				top_translations_of_w.put(w, 1.0); // should be alpha + 1 -alpha * 1. so the result is 1
			}
			HashMap<Integer, Double> dacc = new HashMap<Integer, Double>();

			for(String u : top_translations_of_w.keySet()) {
				LexiconEntry lu = this.lex.getLexiconEntry( u );
				IterablePosting postings = this.invertedIndex.getPostings((BitIndexPointer) lu);
				while (postings.next() != IterablePosting.EOL) {
					int doc = postings.getId();					
					double p_u_d = (double)postings.getFrequency()/(double)postings.getDocumentLength();
					double p_w_u = top_translations_of_w.get(u);
					double sums_p_u_d_p_w_u = 0.0;
					if(dacc.containsKey(doc))
						sums_p_u_d_p_w_u = dacc.get(doc);
					sums_p_u_d_p_w_u = sums_p_u_d_p_w_u + p_u_d * p_w_u;
					dacc.put(doc, sums_p_u_d_p_w_u);
				}
			}
			double colltermFrequency_w = (double)lEntry.getFrequency();
			
			//Iterate through all the docs to score
			for(int doc : dacc.keySet()) {
				double sums_p_u_d_p_w_u = dacc.get(doc);
				IterablePosting postings = di.getPostings(doi.getDocumentEntry(doc));
				double docLength = (double) postings.getDocumentLength();
				
				double score =	
						WeightingModelLibrary.log( (docLength* sums_p_u_d_p_w_u + c * (colltermFrequency_w/numberOfTokens)) / (c + docLength)) 
						- WeightingModelLibrary.log( c/( c+ docLength) * (colltermFrequency_w/numberOfTokens) ) 
						+ WeightingModelLibrary.log(c/(c + docLength))
						;
				if(log_p_d_q[doc]==-1000.0)
					log_p_d_q[doc]=0.0;

				log_p_d_q[doc] = log_p_d_q[doc] +  score;
				
			}
			
		}
			
		//now need to put the scores into the result set
		this.rs.initialise(log_p_d_q);
	}
	
	
	//it is a at query version - this score all documents that contain the translation u
	public void dir_t_mi_atquery_full() throws IOException, InterruptedException {
		double[] log_p_d_q = new double[this.index.getCollectionStatistics().getNumberOfDocuments()];
		Arrays.fill(log_p_d_q, -1000.0);

		PostingIndex<?> di = index.getDirectIndex();
		DocumentIndex doi = index.getDocumentIndex();
		
		double c = this.mu;
		double numberOfTokens = (double) this.index.getCollectionStatistics().getNumberOfTokens();
		
		//iterating over all query terms
		for(int i=0; i<this.queryTerms.length;i++) {
			System.out.println(this.queryTerms[i]);
			String w = this.queryTerms[i];
			LexiconEntry lEntry = this.lex.getLexiconEntry(w);
			if (lEntry==null)
			{
				System.err.println("Term Not Found: "+this.queryTerms[i]);
				continue;
			}
			//IterablePosting ip = this.invertedIndex.getPostings(lEntry);
	
			System.out.println("\t Obtaining translations for " + w + "\t(cf=" + lEntry.getFrequency() + "; docf=" + lEntry.getDocumentFrequency());
			if(lEntry.getFrequency()<this.rarethreshold || lEntry.getDocumentFrequency()<this.rarethreshold 
					|| lEntry.getDocumentFrequency()>this.topthreshold || w.matches(".*\\d+.*"))
				System.out.println("Term " + w + " matches the conditions for not beeing considered by w2v (cf, docf<" + this.rarethreshold + " || docf>" + this.topthreshold+ ")");
			
			
			HashMap<String, Double> top_translations_of_w = getTopMiTranslations_unormalisation(w);
			System.out.println("\t" + top_translations_of_w.size() + " Translations for " + w + " acquired");
			
			
			if(!top_translations_of_w.containsKey(w))
				System.err.println("Attention, there is no self-translation for term " + w);
			//setting the self-translation to 1
			top_translations_of_w.put(w, 1.0);
			
			HashMap<Integer, Double> dacc = new HashMap<Integer, Double>();

			for(String u : top_translations_of_w.keySet()) {
				LexiconEntry lu = this.lex.getLexiconEntry( u );
				IterablePosting postings = this.invertedIndex.getPostings((BitIndexPointer) lu);
				while (postings.next() != IterablePosting.EOL) {
					int doc = postings.getId();					
					double p_u_d = (double)postings.getFrequency()/(double)postings.getDocumentLength();
					double p_w_u = top_translations_of_w.get(u);
					double sums_p_u_d_p_w_u = 0.0;
					if(dacc.containsKey(doc))
						sums_p_u_d_p_w_u = dacc.get(doc);
					sums_p_u_d_p_w_u = sums_p_u_d_p_w_u + p_u_d * p_w_u;
					dacc.put(doc, sums_p_u_d_p_w_u);
				}
			}
			double colltermFrequency_w = (double)lEntry.getFrequency();
			
			//Iterate through all the docs to score
			for(int doc : dacc.keySet()) {
				double sums_p_u_d_p_w_u = dacc.get(doc);
				IterablePosting postings = di.getPostings(doi.getDocumentEntry(doc));
				double docLength = (double) postings.getDocumentLength();
				
				double score =	
						WeightingModelLibrary.log( (docLength* sums_p_u_d_p_w_u + c * (colltermFrequency_w/numberOfTokens)) / (c + docLength)) 
						- WeightingModelLibrary.log( c/( c+ docLength) * (colltermFrequency_w/numberOfTokens) ) 
						+ WeightingModelLibrary.log(c/(c + docLength))
						;
				if(log_p_d_q[doc]==-1000.0)
					log_p_d_q[doc]=0.0;

				log_p_d_q[doc] = log_p_d_q[doc] +  score;
				
			}
			
		}
			
		//now need to put the scores into the result set
		this.rs.initialise(log_p_d_q);
	}
	
	
	//it is a at query version - this score all documents that contain the translation u
		public void dir_t_mi_atquery_full_smethod() throws IOException, InterruptedException {
			double[] log_p_d_q = new double[this.index.getCollectionStatistics().getNumberOfDocuments()];
			Arrays.fill(log_p_d_q, Double.NEGATIVE_INFINITY);

			PostingIndex<?> di = index.getDirectIndex();
			DocumentIndex doi = index.getDocumentIndex();
			
			double c = this.mu;
			double numberOfTokens = (double) this.index.getCollectionStatistics().getNumberOfTokens();
			
			//iterating over all query terms
			for(int i=0; i<this.queryTerms.length;i++) {
				System.out.println(this.queryTerms[i]);
				String w = this.queryTerms[i];
				LexiconEntry lEntry = this.lex.getLexiconEntry(w);
				if (lEntry==null)
				{
					System.err.println("Term Not Found: "+this.queryTerms[i]);
					continue;
				}
				//IterablePosting ip = this.invertedIndex.getPostings(lEntry);
		
				System.out.println("\t Obtaining translations for " + w + "\t(cf=" + lEntry.getFrequency() + "; docf=" + lEntry.getDocumentFrequency());
				if(lEntry.getFrequency()<this.rarethreshold || lEntry.getDocumentFrequency()<this.rarethreshold 
						|| lEntry.getDocumentFrequency()>this.topthreshold || w.matches(".*\\d+.*"))
					System.out.println("Term " + w + " matches the conditions for not beeing considered by w2v (cf, docf<" + this.rarethreshold + " || docf>" + this.topthreshold+ ")");
				
				
				HashMap<String, Double> top_translations_of_w = getTopMiTranslations_unormalisation_smethod(w);
				System.out.println("\t" + top_translations_of_w.size() + " Translations for " + w + " acquired");
				
				
				if(!top_translations_of_w.containsKey(w)) {
					System.err.println("Attention, there is no self-translation for term " + w);
					//setting the self-translation to s
					top_translations_of_w.put(w, this.alpha);
				}
				HashMap<Integer, Double> dacc = new HashMap<Integer, Double>();

				for(String u : top_translations_of_w.keySet()) {
					LexiconEntry lu = this.lex.getLexiconEntry( u );
					IterablePosting postings = this.invertedIndex.getPostings((BitIndexPointer) lu);
					while (postings.next() != IterablePosting.EOL) {
						int doc = postings.getId();					
						double p_u_d = (double)postings.getFrequency()/(double)postings.getDocumentLength();
						double p_w_u = top_translations_of_w.get(u);
						double sums_p_u_d_p_w_u = 0.0;
						if(dacc.containsKey(doc))
							sums_p_u_d_p_w_u = dacc.get(doc);
						sums_p_u_d_p_w_u = sums_p_u_d_p_w_u + p_u_d * p_w_u;
						dacc.put(doc, sums_p_u_d_p_w_u);
					}
				}
				double colltermFrequency_w = (double)lEntry.getFrequency();
				
				//Iterate through all the docs to score
				for(int doc : dacc.keySet()) {
					double sums_p_u_d_p_w_u = dacc.get(doc);
					IterablePosting postings = di.getPostings(doi.getDocumentEntry(doc));
					double docLength = (double) postings.getDocumentLength();
					
					double score =	
							WeightingModelLibrary.log( (docLength* sums_p_u_d_p_w_u + c * (colltermFrequency_w/numberOfTokens)) / (c + docLength)) 
							- WeightingModelLibrary.log( c/( c+ docLength) * (colltermFrequency_w/numberOfTokens) ) 
							+ WeightingModelLibrary.log(c/(c + docLength))
							;
					if(log_p_d_q[doc]==-1000.0)
						log_p_d_q[doc]=0.0;

					log_p_d_q[doc] = log_p_d_q[doc] +  score;
					
				}
				
			}
				
			//now need to put the scores into the result set
			this.rs.initialise(log_p_d_q);
		}
	
	
	
	
	/** Performs retrieval with a Dirichlet smoothing translation language model, where the translation probability is estimated using word2vec
	 * 
	 * This version performs the translation at query time
	 * 
	 * Results are saved in the ResultSet of the object
	 * 
	 * @throws IOException if a problem occurs during matching
	 * @throws InterruptedException 
	 */
	public void dir_t_w2v() throws IOException, InterruptedException {
		MetaIndex meta = index.getMetaIndex();
		double[] log_p_d_q = new double[this.index.getCollectionStatistics().getNumberOfDocuments()];
		Arrays.fill(log_p_d_q, -1000.0);

		//iterating over all query terms
		for(int i=0; i<this.queryTerms.length;i++) {
			System.out.println(this.queryTerms[i]);
			String w = this.queryTerms[i];
			LexiconEntry lEntry = this.lex.getLexiconEntry(w);
			if (lEntry==null)
			{
				System.err.println("Term Not Found: "+this.queryTerms[i]);
				continue;
			}
			IterablePosting ip = this.invertedIndex.getPostings(lEntry);
	
			System.out.println("\t Obtaining translations for " + w + "\t(cf=" + lEntry.getFrequency() + "; docf=" + lEntry.getDocumentFrequency());
			if(lEntry.getFrequency()<this.rarethreshold || lEntry.getDocumentFrequency()<this.rarethreshold 
					|| lEntry.getDocumentFrequency()>this.topthreshold || w.matches(".*\\d+.*"))
				System.out.println("Term " + w + " matches the conditions for not beeing considered by w2v (cf, docf<" + this.rarethreshold + " || docf>" + this.topthreshold);
			
			
			//HashMap<String, Double> top_translations_of_w = getTopW2VTranslations(w); // <- this is the line to change if we want other transltions
			HashMap<String, Double> top_translations_of_w = getTopW2VTranslations_atquerytime(w);
			System.out.println("\t" + top_translations_of_w.size() + " Translations for " + w + " acquired");
			
			/*Preload the probabilities p(u|d)*/
			HashMap<String, HashMap<Integer, Double>> p_u_d_distributions = new HashMap<String, HashMap<Integer, Double>>();
			for(String u : top_translations_of_w.keySet()) {
				Index index = Index.createIndex();
				LexiconEntry lu = this.lex.getLexiconEntry( u );
				IterablePosting postings = this.invertedIndex.getPostings((BitIndexPointer) lu);
				HashMap<Integer, Double> u_distribution = new HashMap<Integer, Double>();
				while (postings.next() != IterablePosting.EOL) {
					int doc = postings.getId();
					double p_u_d = (double)postings.getFrequency()/(double)postings.getDocumentLength();
					u_distribution.put(doc, p_u_d);
				}
				p_u_d_distributions.put(u, u_distribution);
			}
			
			/*Iterate over all the docs that contain w (iterating over all documents in the posting of this query term)
			 * 
			 * Note this only does best match style of Dirichlet
			 * while this is good for single term queries, it is incorrect for multi term queries
			 * */
			while(ip.next() != IterablePosting.EOL) {
				{
					double tf = (double)ip.getFrequency();
					double c = this.mu;
					double numberOfTokens = (double) this.index.getCollectionStatistics().getNumberOfTokens();
					double docLength = (double) ip.getDocumentLength();
					double colltermFrequency = (double)lEntry.getFrequency();
					
					double sum_p_t_w_u = 0.0;
					for(String u : top_translations_of_w.keySet()) {
						double p_t_w_u = top_translations_of_w.get(u);
						//System.out.println("Translation to " + u + " -> " + p_t_w_u);
						/*double p_u_d=0.0;
						PostingIndex di = index.getDirectIndex();
						DocumentIndex doi = index.getDocumentIndex();
						IterablePosting postings = di.getPostings((BitIndexPointer)doi.getDocumentEntry(ip.getId()));
						while (postings.next() != IterablePosting.EOL) {
							Map.Entry<String,LexiconEntry> lee = lex.getLexiconEntry(postings.getId());
							if(lee.getKey().equalsIgnoreCase(u)) {
								p_u_d = (double)postings.getFrequency()/docLength;
								break;
							}
						}*/
						double p_u_d=0.0;
						if(p_u_d_distributions.get(u).containsKey(ip.getId()))
							p_u_d= p_u_d_distributions.get(u).get(ip.getId());
						//System.out.println("Prob u in doc " + p_u_d);
						sum_p_t_w_u = sum_p_t_w_u + p_t_w_u * p_u_d;
					}
					//System.out.println("sum_p_t_w_u=" + sum_p_t_w_u);
					
					double score =	
							WeightingModelLibrary.log( (docLength* sum_p_t_w_u + c * (colltermFrequency/numberOfTokens)) / (c + docLength)) 
							- WeightingModelLibrary.log( c/( c+ docLength) * (colltermFrequency/numberOfTokens) ) 
							+ WeightingModelLibrary.log(c/(c + docLength))
							;

					/*tring docno = meta.getItem("docno", ip.getId());
					System.out.println(docno + "\t->\t" + score);
					System.out.println("log_p_d_q[ip.getId()]=" + log_p_d_q[ip.getId()] + "\t" + 
							"docLen=" + docLength + "\ttf=" + tf
							+ "\tcf=" + colltermFrequency + "\tN=" + numberOfTokens + "\tmu="+this.mu
							);*/
					
					if(log_p_d_q[ip.getId()]==-1000.0)
						log_p_d_q[ip.getId()]=0.0;

					log_p_d_q[ip.getId()] = log_p_d_q[ip.getId()] +  score;

				}
			}
		}
		//now need to put the scores into the result set
		this.rs.initialise(log_p_d_q);
	}
	
	
	/** Performs retrieval with a Dirichlet smoothing translation language model, where the translation probability is estimated using word2vec
	 * 
	 * This version performs the translation at query time
	 * 
	 * Results are saved in the ResultSet of the object
	 * 
	 * @throws IOException if a problem occurs during matching
	 * @throws InterruptedException 
	 */
	public void dir_t_w2v_full() throws IOException, InterruptedException {
		double[] log_p_d_q = new double[this.index.getCollectionStatistics().getNumberOfDocuments()];
		Arrays.fill(log_p_d_q, -1000.0);

		PostingIndex<?> di = index.getDirectIndex();
		DocumentIndex doi = index.getDocumentIndex();
		
		double c = this.mu;
		double numberOfTokens = (double) this.index.getCollectionStatistics().getNumberOfTokens();
		
		//iterating over all query terms
		for(int i=0; i<this.queryTerms.length;i++) {
			System.out.println(this.queryTerms[i]);
			String w = this.queryTerms[i];
			LexiconEntry lEntry = this.lex.getLexiconEntry(w);
			if (lEntry==null)
			{
				System.err.println("Term Not Found: "+this.queryTerms[i]);
				continue;
			}
			//IterablePosting ip = this.invertedIndex.getPostings(lEntry);
	
			System.out.println("\t Obtaining translations for " + w + "\t(cf=" + lEntry.getFrequency() + "; docf=" + lEntry.getDocumentFrequency());
			if(lEntry.getFrequency()<this.rarethreshold || lEntry.getDocumentFrequency()<this.rarethreshold 
					|| lEntry.getDocumentFrequency()>this.topthreshold || w.matches(".*\\d+.*"))
				System.out.println("Term " + w + " matches the conditions for not beeing considered by w2v (cf, docf<" + this.rarethreshold + " || docf>" + this.topthreshold+ ")");
			
			
			//HashMap<String, Double> top_translations_of_w = getTopW2VTranslations_atquerytime_notnormalised(w); 
			HashMap<String, Double> top_translations_of_w = getTopW2VTranslations_atquerytime(w);
			System.out.println("\t" + top_translations_of_w.size() + " Translations for " + w + " acquired");
			
			
			if(!top_translations_of_w.containsKey(w))
				System.err.println("Attention, there is no self-translation for term " + w);
			//setting the self-translation to 1
			top_translations_of_w.put(w, 1.0);
			
			/*FOR ANALYSIS ONLY*/

			/*END OF ANALYSIS CODE*/
			
			
			HashMap<Integer, Double> dacc = new HashMap<Integer, Double>();

			for(String u : top_translations_of_w.keySet()) {
				LexiconEntry lu = this.lex.getLexiconEntry( u );
				IterablePosting postings = this.invertedIndex.getPostings((BitIndexPointer) lu);
				while (postings.next() != IterablePosting.EOL) {
					int doc = postings.getId();					
					double p_u_d = (double)postings.getFrequency()/(double)postings.getDocumentLength();
					double p_w_u = top_translations_of_w.get(u);
					/*FOR ANALYSIS ONLY*/
					/*if(u.equalsIgnoreCase("dollar"))
						p_w_u=0;*/
					/*END OF ANALYSIS CODE*/

					double sums_p_u_d_p_w_u = 0.0;
					if(dacc.containsKey(doc))
						sums_p_u_d_p_w_u = dacc.get(doc);
					sums_p_u_d_p_w_u = sums_p_u_d_p_w_u + p_u_d * p_w_u;
					dacc.put(doc, sums_p_u_d_p_w_u);
				}
			}
			double colltermFrequency_w = (double)lEntry.getFrequency();
			
			//Iterate through all the docs to score
			for(int doc : dacc.keySet()) {
				double sums_p_u_d_p_w_u = dacc.get(doc);
				IterablePosting postings = di.getPostings(doi.getDocumentEntry(doc));
				double docLength = (double) postings.getDocumentLength();
				
				double score =	
						WeightingModelLibrary.log( (docLength* sums_p_u_d_p_w_u + c * (colltermFrequency_w/numberOfTokens)) / (c + docLength)) 
						- WeightingModelLibrary.log( c/( c+ docLength) * (colltermFrequency_w/numberOfTokens) ) 
						+ WeightingModelLibrary.log(c/(c + docLength))
						;
				if(log_p_d_q[doc]==-1000.0)
					log_p_d_q[doc]=0.0;

				log_p_d_q[doc] = log_p_d_q[doc] +  score;
				
			}
			
		}
			
		//now need to put the scores into the result set
		this.rs.initialise(log_p_d_q);
	}
	
	
	
	/** Performs retrieval with a Dirichlet smoothing translation language model, where the translation probability is estimated using word2vec
	 * 
	 * This version performs the translation at query time
	 * 
	 * Results are saved in the ResultSet of the object
	 * 
	 * @throws IOException if a problem occurs during matching
	 * @throws InterruptedException 
	 */
	public void dir_t_w2v_full_altIndex() throws IOException, InterruptedException {
		double[] log_p_d_q = new double[this.index.getCollectionStatistics().getNumberOfDocuments()];
		Arrays.fill(log_p_d_q, -1000.0);

		PostingIndex<?> di = index.getDirectIndex();
		DocumentIndex doi = index.getDocumentIndex();
		
		double c = this.mu;
		double numberOfTokens = (double) this.index.getCollectionStatistics().getNumberOfTokens();
		
		//iterating over all query terms
		for(int i=0; i<this.queryTerms.length;i++) {
			System.out.println(this.queryTerms[i]);
			String w = this.queryTerms[i];
			LexiconEntry lEntry = this.lex.getLexiconEntry(w);
			if (lEntry==null)
			{
				System.err.println("Term Not Found: "+this.queryTerms[i]);
				continue;
			}
			//IterablePosting ip = this.invertedIndex.getPostings(lEntry);
	
			System.out.println("\t Obtaining translations for " + w + "\t(cf=" + lEntry.getFrequency() + "; docf=" + lEntry.getDocumentFrequency());
			if(lEntry.getFrequency()<this.rarethreshold || lEntry.getDocumentFrequency()<this.rarethreshold 
					|| lEntry.getDocumentFrequency()>this.topthreshold || w.matches(".*\\d+.*"))
				System.out.println("Term " + w + " matches the conditions for not beeing considered by w2v (cf, docf<" + this.rarethreshold + " || docf>" + this.topthreshold+ ")");
			
			HashMap<String, Double> top_translations_of_w = getTopW2VTranslations_atquerytime(w);
			System.out.println("\t" + top_translations_of_w.size() + " Translations for " + w + " acquired");
			
			
			if(!top_translations_of_w.containsKey(w))
				System.err.println("Attention, there is no self-translation for term " + w);
			//setting the self-translation to 1
			top_translations_of_w.put(w, 1.0);
			
			HashMap<Integer, Double> dacc = new HashMap<Integer, Double>();

			for(String u : top_translations_of_w.keySet()) {
				LexiconEntry lu = this.lex.getLexiconEntry( u );
				IterablePosting postings = this.invertedIndex.getPostings((BitIndexPointer) lu);
				while (postings.next() != IterablePosting.EOL) {
					int doc = postings.getId();					
					double p_u_d = (double)postings.getFrequency()/(double)postings.getDocumentLength();
					double p_w_u = top_translations_of_w.get(u);
					double sums_p_u_d_p_w_u = 0.0;
					if(dacc.containsKey(doc))
						sums_p_u_d_p_w_u = dacc.get(doc);
					sums_p_u_d_p_w_u = sums_p_u_d_p_w_u + p_u_d * p_w_u;
					dacc.put(doc, sums_p_u_d_p_w_u);
				}
			}
			double colltermFrequency_w = (double)lEntry.getFrequency();
			
			//Iterate through all the docs to score
			for(int doc : dacc.keySet()) {
				double sums_p_u_d_p_w_u = dacc.get(doc);
				IterablePosting postings = di.getPostings(doi.getDocumentEntry(doc));
				double docLength = (double) postings.getDocumentLength();
				
				double score =	
						WeightingModelLibrary.log( (docLength* sums_p_u_d_p_w_u + c * (colltermFrequency_w/numberOfTokens)) / (c + docLength)) 
						- WeightingModelLibrary.log( c/( c+ docLength) * (colltermFrequency_w/numberOfTokens) ) 
						+ WeightingModelLibrary.log(c/(c + docLength))
						;
				if(log_p_d_q[doc]==-1000.0)
					log_p_d_q[doc]=0.0;

				log_p_d_q[doc] = log_p_d_q[doc] +  score;
				
			}
			
		}
			
		//now need to put the scores into the result set
		this.rs.initialise(log_p_d_q);
	}

	
	
	
	/** Performs retrieval with a Dirichlet smoothing translation language model, where the translation probability is estimated using word2vec
	 * 
	 * This version performs the translation at query time
	 * 
	 * Results are saved in the ResultSet of the object
	 * 
	 * @throws IOException if a problem occurs during matching
	 * @throws InterruptedException 
	 */
	public void dir_t_w2v_self() throws IOException, InterruptedException {
		MetaIndex meta = index.getMetaIndex();
		double[] log_p_d_q = new double[this.index.getCollectionStatistics().getNumberOfDocuments()];
		Arrays.fill(log_p_d_q, -1000.0);

		//iterating over all query terms
		for(int i=0; i<this.queryTerms.length;i++) {
			System.out.println(this.queryTerms[i]);
			String w = this.queryTerms[i];
			LexiconEntry lEntry = this.lex.getLexiconEntry(w);
			if (lEntry==null)
			{
				System.err.println("Term Not Found: "+this.queryTerms[i]);
				continue;
			}
			IterablePosting ip = this.invertedIndex.getPostings(lEntry);
	
			System.out.println("\t Obtaining translations for " + w + "\t(cf=" + lEntry.getFrequency() + "; docf=" + lEntry.getDocumentFrequency());
			if(lEntry.getFrequency()<this.rarethreshold || lEntry.getDocumentFrequency()<this.rarethreshold 
					|| lEntry.getDocumentFrequency()>this.topthreshold || w.matches(".*\\d+.*"))
				System.out.println("Term " + w + " matches the conditions for not beeing considered by w2v (cf, docf<" + this.rarethreshold + " || docf>" + this.topthreshold);
			
			
			//HashMap<String, Double> top_translations_of_w = getTopW2VTranslations(w); // <- this is the line to change if we want other transltions
			HashMap<String, Double> top_translations_of_w = getTopW2VTranslations_atquerytime(w);
			System.out.println("\t" + top_translations_of_w.size() + " Translations for " + w + " acquired");
			
			/*Preload the probabilities p(u|d)*/
			HashMap<String, HashMap<Integer, Double>> p_u_d_distributions = new HashMap<String, HashMap<Integer, Double>>();
			for(String u : top_translations_of_w.keySet()) {
				Index index = Index.createIndex();
				LexiconEntry lu = this.lex.getLexiconEntry( u );
				IterablePosting postings = this.invertedIndex.getPostings((BitIndexPointer) lu);
				HashMap<Integer, Double> u_distribution = new HashMap<Integer, Double>();
				while (postings.next() != IterablePosting.EOL) {
					int doc = postings.getId();
					double p_u_d = (double)postings.getFrequency()/(double)postings.getDocumentLength();
					u_distribution.put(doc, p_u_d);
				}
				p_u_d_distributions.put(u, u_distribution);
			}
			
			/*Iterate over all the docs that contain w (iterating over all documents in the posting of this query term)
			 * 
			 * Note this only does best match style of Dirichlet
			 * while this is good for single term queries, it is incorrect for multi term queries
			 * */
			while(ip.next() != IterablePosting.EOL) {
				{
					double tf = (double)ip.getFrequency();
					double c = this.mu;
					double numberOfTokens = (double) this.index.getCollectionStatistics().getNumberOfTokens();
					double docLength = (double) ip.getDocumentLength();
					double colltermFrequency = (double)lEntry.getFrequency();
					
					double sum_p_t_w_u = 0.0;
					for(String u : top_translations_of_w.keySet()) {
						double p_t_w_u = top_translations_of_w.get(u);
						//System.out.println("Translation to " + u + " -> " + p_t_w_u);
						/*double p_u_d=0.0;
						PostingIndex di = index.getDirectIndex();
						DocumentIndex doi = index.getDocumentIndex();
						IterablePosting postings = di.getPostings((BitIndexPointer)doi.getDocumentEntry(ip.getId()));
						while (postings.next() != IterablePosting.EOL) {
							Map.Entry<String,LexiconEntry> lee = lex.getLexiconEntry(postings.getId());
							if(lee.getKey().equalsIgnoreCase(u)) {
								p_u_d = (double)postings.getFrequency()/docLength;
								break;
							}
						}*/
						double p_u_d=0.0;
						if(p_u_d_distributions.get(u).containsKey(ip.getId()))
							p_u_d= p_u_d_distributions.get(u).get(ip.getId());
						//System.out.println("Prob u in doc " + p_u_d);
						
						//set self translation to 1
						if(u.equals(w))
							p_t_w_u=1;
						
						sum_p_t_w_u = sum_p_t_w_u + p_t_w_u * p_u_d;
					}
					//System.out.println("sum_p_t_w_u=" + sum_p_t_w_u);
					
					double score =	
							WeightingModelLibrary.log( (docLength* sum_p_t_w_u + c * (colltermFrequency/numberOfTokens)) / (c + docLength)) 
							- WeightingModelLibrary.log( c/( c+ docLength) * (colltermFrequency/numberOfTokens) ) 
							+ WeightingModelLibrary.log(c/(c + docLength))
							;

					/*tring docno = meta.getItem("docno", ip.getId());
					System.out.println(docno + "\t->\t" + score);
					System.out.println("log_p_d_q[ip.getId()]=" + log_p_d_q[ip.getId()] + "\t" + 
							"docLen=" + docLength + "\ttf=" + tf
							+ "\tcf=" + colltermFrequency + "\tN=" + numberOfTokens + "\tmu="+this.mu
							);*/
					
					if(log_p_d_q[ip.getId()]==-1000.0)
						log_p_d_q[ip.getId()]=0.0;

					log_p_d_q[ip.getId()] = log_p_d_q[ip.getId()] +  score;

				}
			}
		}
		//now need to put the scores into the result set
		this.rs.initialise(log_p_d_q);
	}
	
	
	
	/** Performs retrieval with a Dirichlet smoothing translation language model, where the translation probability is estimated using word2vec
	 * 
	 * This version performs the translation at query time
	 * 
	 * Results are saved in the ResultSet of the object
	 * 
	 * @throws IOException if a problem occurs during matching
	 * @throws InterruptedException 
	 */
	public void dir_t_w2v_notnormalised() throws IOException, InterruptedException {
		MetaIndex meta = index.getMetaIndex();
		double[] log_p_d_q = new double[this.index.getCollectionStatistics().getNumberOfDocuments()];
		Arrays.fill(log_p_d_q, -1000.0);

		//iterating over all query terms
		for(int i=0; i<this.queryTerms.length;i++) {
			System.out.println(this.queryTerms[i]);
			String w = this.queryTerms[i];
			LexiconEntry lEntry = this.lex.getLexiconEntry(w);
			if (lEntry==null)
			{
				System.err.println("Term Not Found: "+this.queryTerms[i]);
				continue;
			}
			IterablePosting ip = this.invertedIndex.getPostings(lEntry);
	
			System.out.println("\t Obtaining translations for " + w + "\t(cf=" + lEntry.getFrequency() + "; docf=" + lEntry.getDocumentFrequency());
			if(lEntry.getFrequency()<this.rarethreshold || lEntry.getDocumentFrequency()<this.rarethreshold 
					|| lEntry.getDocumentFrequency()>this.topthreshold || w.matches(".*\\d+.*"))
				System.out.println("Term " + w + " matches the conditions for not beeing considered by w2v (cf, docf<" + this.rarethreshold + " || docf>" + this.topthreshold);
			
			
			//HashMap<String, Double> top_translations_of_w = getTopW2VTranslations(w); // <- this is the line to change if we want other transltions
			HashMap<String, Double> top_translations_of_w = getTopW2VTranslations_atquerytime_notnormalised(w);
			System.out.println("\t" + top_translations_of_w.size() + " Translations for " + w + " acquired");
			
			/*Preload the probabilities p(u|d)*/
			HashMap<String, HashMap<Integer, Double>> p_u_d_distributions = new HashMap<String, HashMap<Integer, Double>>();
			for(String u : top_translations_of_w.keySet()) {
				Index index = Index.createIndex();
				LexiconEntry lu = this.lex.getLexiconEntry( u );
				IterablePosting postings = this.invertedIndex.getPostings((BitIndexPointer) lu);
				HashMap<Integer, Double> u_distribution = new HashMap<Integer, Double>();
				while (postings.next() != IterablePosting.EOL) {
					int doc = postings.getId();
					double p_u_d = (double)postings.getFrequency()/(double)postings.getDocumentLength();
					u_distribution.put(doc, p_u_d);
				}
				p_u_d_distributions.put(u, u_distribution);
			}
			
			/*Iterate over all the docs that contain w (iterating over all documents in the posting of this query term)
			 * 
			 * Note this only does best match style of Dirichlet
			 * while this is good for single term queries, it is incorrect for multi term queries
			 * */
			while(ip.next() != IterablePosting.EOL) {
				{
					double tf = (double)ip.getFrequency();
					double c = this.mu;
					double numberOfTokens = (double) this.index.getCollectionStatistics().getNumberOfTokens();
					double docLength = (double) ip.getDocumentLength();
					double colltermFrequency = (double)lEntry.getFrequency();
					
					double sum_p_t_w_u = 0.0;
					for(String u : top_translations_of_w.keySet()) {
						double p_t_w_u = top_translations_of_w.get(u);
						//System.out.println("Translation to " + u + " -> " + p_t_w_u);
						/*double p_u_d=0.0;
						PostingIndex di = index.getDirectIndex();
						DocumentIndex doi = index.getDocumentIndex();
						IterablePosting postings = di.getPostings((BitIndexPointer)doi.getDocumentEntry(ip.getId()));
						while (postings.next() != IterablePosting.EOL) {
							Map.Entry<String,LexiconEntry> lee = lex.getLexiconEntry(postings.getId());
							if(lee.getKey().equalsIgnoreCase(u)) {
								p_u_d = (double)postings.getFrequency()/docLength;
								break;
							}
						}*/
						double p_u_d=0.0;
						if(p_u_d_distributions.get(u).containsKey(ip.getId()))
							p_u_d= p_u_d_distributions.get(u).get(ip.getId());
						//System.out.println("Prob u in doc " + p_u_d);
						sum_p_t_w_u = sum_p_t_w_u + p_t_w_u * p_u_d;
					}
					//System.out.println("sum_p_t_w_u=" + sum_p_t_w_u);
					
					double score =	
							WeightingModelLibrary.log( (docLength* sum_p_t_w_u + c * (colltermFrequency/numberOfTokens)) / (c + docLength)) 
							- WeightingModelLibrary.log( c/( c+ docLength) * (colltermFrequency/numberOfTokens) ) 
							+ WeightingModelLibrary.log(c/(c + docLength))
							;

					/*tring docno = meta.getItem("docno", ip.getId());
					System.out.println(docno + "\t->\t" + score);
					System.out.println("log_p_d_q[ip.getId()]=" + log_p_d_q[ip.getId()] + "\t" + 
							"docLen=" + docLength + "\ttf=" + tf
							+ "\tcf=" + colltermFrequency + "\tN=" + numberOfTokens + "\tmu="+this.mu
							);*/
					
					if(log_p_d_q[ip.getId()]==-1000.0)
						log_p_d_q[ip.getId()]=0.0;

					log_p_d_q[ip.getId()] = log_p_d_q[ip.getId()] +  score;

				}
			}
		}
		//now need to put the scores into the result set
		this.rs.initialise(log_p_d_q);
	}
	
	
	double[] get_empty_sentence_vector() {
		int dimensions =0;
		for (String s: fullw2vmatrix.keySet()) {
			dimensions = fullw2vmatrix.get(s).length;
			break;
		}
		double[] sentence_vector = new double[dimensions];
		return sentence_vector;
	}
	
	double[] add_to_sentence_vector(double[] sentence_vector, String w) {
		if(this.fullw2vmatrix.containsKey(w)) {
			double[] vector_w = this.fullw2vmatrix.get(w);
			for(int i=0; i<vector_w.length; i++) {
				sentence_vector[i] = sentence_vector[i] + vector_w[i];
			}
		}
		return sentence_vector;
	}
	
	/** Performs retrieval with a Dirichlet smoothing translation language model, where the translation probability is estimated using word2vec
	 * 
	 * This version performs the translation at query time
	 * 
	 * Results are saved in the ResultSet of the object
	 * 
	 * @throws IOException if a problem occurs during matching
	 * @throws InterruptedException 
	 */
	public void dir_t_w2v_phrase() throws IOException, InterruptedException {
		MetaIndex meta = index.getMetaIndex();
		double[] log_p_d_q = new double[this.index.getCollectionStatistics().getNumberOfDocuments()];
		Arrays.fill(log_p_d_q, -1000.0);

		//I need to acquire here the top translations for the query as a phrase (ie perform additive operations)
		double[] sentence_vector = get_empty_sentence_vector();
		
		/*Preload the probabilities p(u|d)*/
		HashMap<String, HashMap<Integer, Double>> p_u_d_distributions = new HashMap<String, HashMap<Integer, Double>>();
		
		//build the sentence vector
		for(int i=0; i<this.queryTerms.length;i++) {
			System.out.println(this.queryTerms[i]);
			String w = this.queryTerms[i];
			sentence_vector = add_to_sentence_vector(sentence_vector, w);	
		}
		//now do the phrase translation
		HashMap<String, Double> top_translations_of_sentence = getTopW2VTranslations_atquerytime_phrase(sentence_vector);
		System.out.println("\t" + top_translations_of_sentence.size() + " Translations for the sentence vector acquired");
		for(String u : top_translations_of_sentence.keySet()) {
			Index index = Index.createIndex();
			LexiconEntry lu = this.lex.getLexiconEntry( u );
			IterablePosting postings = this.invertedIndex.getPostings((BitIndexPointer) lu);
			HashMap<Integer, Double> u_distribution = new HashMap<Integer, Double>();
			while (postings.next() != IterablePosting.EOL) {
				int doc = postings.getId();
				double p_u_d = (double)postings.getFrequency()/(double)postings.getDocumentLength();
				u_distribution.put(doc, p_u_d);
			}
			p_u_d_distributions.put(u, u_distribution);
		}
		
		
		
		
		//iterating over all query terms
		for(int i=0; i<this.queryTerms.length;i++) {
			System.out.println(this.queryTerms[i]);
			String w = this.queryTerms[i];
			LexiconEntry lEntry = this.lex.getLexiconEntry(this.queryTerms[i]);
			if (lEntry==null)
			{
				System.err.println("Term Not Found: "+this.queryTerms[i]);
				continue;
			}
			double p_w_C = (double)lEntry.getFrequency()/this.index.getCollectionStatistics().getNumberOfTokens();
			IterablePosting ip = this.invertedIndex.getPostings(lEntry);

			//iterating over all documents in the posting of this query term


			/*Note this only does best match style of Dirichlet
			 * while this is good for single term queries, it is incorrect for multi term queries
			 * */
			while(ip.next() != IterablePosting.EOL) {
				{
					double tf = (double)ip.getFrequency();
					double c = this.mu;
					double numberOfTokens = (double) this.index.getCollectionStatistics().getNumberOfTokens();
					double docLength = (double) ip.getDocumentLength();
					double colltermFrequency = (double)lEntry.getFrequency();
					
					double sum_p_t_w_u = 0.0;
					for(String u : top_translations_of_sentence.keySet()) {
						double p_t_w_u = top_translations_of_sentence.get(u);
						//System.out.println("Translation to " + u + " -> " + p_t_w_u);
						/*double p_u_d=0.0;
						PostingIndex di = index.getDirectIndex();
						DocumentIndex doi = index.getDocumentIndex();
						IterablePosting postings = di.getPostings((BitIndexPointer)doi.getDocumentEntry(ip.getId()));
						while (postings.next() != IterablePosting.EOL) {
							Map.Entry<String,LexiconEntry> lee = lex.getLexiconEntry(postings.getId());
							if(lee.getKey().equalsIgnoreCase(u)) {
								p_u_d = (double)postings.getFrequency()/docLength;
								break;
							}
						}*/
						double p_u_d=0.0;
						if(p_u_d_distributions.get(u).containsKey(ip.getId()))
							p_u_d= p_u_d_distributions.get(u).get(ip.getId());
						//System.out.println("Prob u in doc " + p_u_d);
						if(u.equalsIgnoreCase(w))
							p_t_w_u = 1;
						sum_p_t_w_u = sum_p_t_w_u + p_t_w_u * p_u_d;
					}
					//System.out.println("sum_p_t_w_u=" + sum_p_t_w_u);
					
					//this checks if the self-translation took place or not
					if(!top_translations_of_sentence.containsKey(w))
						sum_p_t_w_u = sum_p_t_w_u + tf/docLength;
					
					
					double score =	
							WeightingModelLibrary.log( (docLength* sum_p_t_w_u + c * (colltermFrequency/numberOfTokens)) / (c + docLength)) 
							- WeightingModelLibrary.log( c/( c+ docLength) * (colltermFrequency/numberOfTokens) ) 
							+ WeightingModelLibrary.log(c/(c + docLength))
							;

					/*tring docno = meta.getItem("docno", ip.getId());
					System.out.println(docno + "\t->\t" + score);
					System.out.println("log_p_d_q[ip.getId()]=" + log_p_d_q[ip.getId()] + "\t" + 
							"docLen=" + docLength + "\ttf=" + tf
							+ "\tcf=" + colltermFrequency + "\tN=" + numberOfTokens + "\tmu="+this.mu
							);*/
					
					if(log_p_d_q[ip.getId()]==-1000.0)
						log_p_d_q[ip.getId()]=0.0;

					log_p_d_q[ip.getId()] = log_p_d_q[ip.getId()] +  score;

				}
			}
		}
		
		
		
		
		//now need to put the scores into the result set
		this.rs.initialise(log_p_d_q);
	}

	
	
	public HashMap<String, Double> getTopW2VTranslations_atquerytime_phrase(double[] sentence_vector) {
		TreeMultimap<Double, String> inverted_translation_sentence = TreeMultimap.create(Ordering.natural().reverse(), Ordering.natural());
		HashMap<String, Double> sentence_top_cooccurence = new HashMap<String, Double>();
		
		
		HashMap<String,Double> tmp_sentence = new HashMap<String,Double>();
		double sum_cosines=0.0;
		
		for(String u : this.fullw2vmatrix.keySet()) {
			double[] vector_u = fullw2vmatrix.get(u);
			double p_u_sentence=0.0;
			double cosine_sentence_u=0.0;
			double sum_sentence=0.0;
			double sum_u=0.0;
			for(int i=0; i<vector_u.length; i++) {
				cosine_sentence_u=cosine_sentence_u + vector_u[i]*sentence_vector[i];
				sum_sentence=sum_sentence + Math.pow(sentence_vector[i],2);
				sum_u=sum_u + Math.pow(vector_u[i],2);
			}
			cosine_sentence_u = cosine_sentence_u / (Math.sqrt(sum_sentence) * Math.sqrt(sum_u));
			//next is done for normalising
			tmp_sentence.put(u, cosine_sentence_u);
			sum_cosines = sum_cosines+ cosine_sentence_u;
		}
		

		//normalise cosine to probabilities and insert in order
		for(String u: tmp_sentence.keySet()) {
			double p_w2v_sentence_u = tmp_sentence.get(u)/sum_cosines;
			inverted_translation_sentence.put(p_w2v_sentence_u, u);
		}
		//w2v_inverted_translation.put("sentence", inverted_translation_sentence);
		
		
		//select to T translations
		double sums_u=0.0;
		int count=0;
		for (Double p_w_u : inverted_translation_sentence.keySet()) {
			if(count<this.number_of_top_translation_terms) {
				NavigableSet<String> terms = inverted_translation_sentence.get(p_w_u);
				Iterator<String> termit = terms.iterator();
				while(termit.hasNext()) {
					String topterm = termit.next();
					if(count<this.number_of_top_translation_terms) {
						sentence_top_cooccurence.put(topterm, p_w_u);
						sums_u=sums_u + p_w_u;
						count ++;
					}else
						break;
				}
			}else
				break;
		}

		//normalised based on u
		HashMap<String, Double> tmp_w_top_cooccurence = new HashMap<String, Double>();
		int tcount=0;
		double cumsum=0.0;
		for(String u: sentence_top_cooccurence.keySet()) {
			tmp_w_top_cooccurence.put(u, sentence_top_cooccurence.get(u)/sums_u);
			System.out.println("\t  " + sentence_top_cooccurence.get(u)/sums_u + ": " + u);
			cumsum=cumsum+sentence_top_cooccurence.get(u)/sums_u;
			tcount++;
		}
		System.out.println(tcount + " translations selected, for a cumulative sum of " + cumsum);
		return tmp_w_top_cooccurence;
	}
	
	
	/** Performs retrieval with a Dirichlet smoothing translation language model (or no translation). The translation model is specified by the parameter and can be:
	 * - null: no translation, thus resembling a standard Dirichelt LM
	 * - mi: mutual information, as described in Karimzadehgan, Zhai, "Estimation of Statistical Translation Models Based on Mutual Information for Ad Hoc Information Retrieval", SIGIR 2010
	 * - parametric_mi: mutual information with parametric setting of self-translation probabilities, as described in Karimzadehgan, Zhai, "Estimation of Statistical Translation Models Based on Mutual Information for Ad Hoc Information Retrieval", SIGIR 2010
	 * 
	 * Results are saved in the ResultSet of the object
	 * 
	 * @param translationLMMethod - the type of method used to generate translations and estimate probabilities
	 * @throws IOException if a problem occurs during matching
	 * @throws InterruptedException 
	 */
	public void dir(String translationLMMethod) throws IOException, InterruptedException {
		switch (translationLMMethod.toLowerCase()) {
        case "null":
            dir();
            break;
            /*TODO: complete with other translation methods*/
        case "dir_theory":
            dir_theory();
            break;
        case "mi":
        	dir_t_mi_atquery();
            break;
        case "mi_self":
        	dir_t_mi_atquery_self();
            break;
        case "mi_alpha":
        	dir_t_mi_atquery_withalpha();
            break;
        case "mi_full":
        	dir_t_mi_atquery_full();
            break;   
        case "mi_full_window":
        	dir_t_mi_atquery_full();
            break;   
        case "mi_full_smethod":
        	dir_t_mi_atquery_full_smethod();
            break;  
        case "w2v":
        	dir_t_w2v();
            break;
        case "w2v_phrase":
        	dir_t_w2v_phrase();
            break;
        case "w2v_notnorm":
        	dir_t_w2v_notnormalised();
            break;  
        case "w2v_self":
        	dir_t_w2v_self();
            break; 
        case "w2v_full":
        	dir_t_w2v_full();
            break; 
        default: 
        	dir();
        	System.err.println("No translation explicitely set!");
            break;
		}
	}
	
	public void jm(String translationLMMethod) {
		/*TODO: finish*/
	}
	
	public void score(String translationLMMethod, String smoothing) throws IOException, InterruptedException {
		switch (smoothing.toLowerCase()) {
        case "dir":
            dir(translationLMMethod);
            break;
        case "jm":
        	jm(translationLMMethod);
            break;
        default: 
        	dir(translationLMMethod);
            break;
		}
	}

	public Request runMatching(Request srq, String translationLMMethod, String smoothing) throws IOException, InterruptedException {
		String query = srq.getOriginalQuery(); //this is before any pre-processing
		System.out.println("query: " + query);
		this.queryTerms = srq.getOriginalQuery().split(" ");
		
		ResultSet rs = new CollectionResultSet(this.index.getCollectionStatistics().getNumberOfDocuments());
		score(translationLMMethod, smoothing);
		this.rs.sort();
		srq.setResultSet(this.rs);
		return srq;
		
	}
	
	//returns the documents were w occurs in - binary version
	Set<Integer> occursin_binary(String v) throws IOException{
		Set<Integer> vecv = new HashSet<Integer>();
		Lexicon<String> lex = index.getLexicon();
		LexiconEntry le = lex.getLexiconEntry( v );
		IterablePosting postings = invertedIndex.getPostings( le);
		while (postings.next() != IterablePosting.EOL) {
			vecv.add(postings.getId());
		}
		return vecv;
	}
	
	
	HashMap<String, Double> getMiTranslations(String w) throws IOException {
		Lexicon<String> lex = index.getLexicon();
		double N = (double)index.getCollectionStatistics().getNumberOfDocuments();
		HashMap<String, Double> w_cooccurence = new HashMap<String, Double>();
		Set<Integer> docsofw = occursin_binary(w);
		Iterator<Entry <String, LexiconEntry> > itu = lex.iterator();
		LexiconEntry lew = lex.getLexiconEntry(w);
		double p_Xw_1 = (double)lew.getDocumentFrequency()/N;
		double p_Xw_0 = 1.0-p_Xw_1;
		while(itu.hasNext()) {
			Entry<String, LexiconEntry> lu = itu.next();
			String termu = lu.getKey();
			if(lu.getValue().getFrequency()<this.rarethreshold || lu.getValue().getFrequency()>this.topthreshold)
				continue;
			
			//System.out.println("\tmeasuring co-occurence with " + termu);
			//LexiconEntry leu = lu.getValue();
			Set<Integer> docsofu = occursin_binary(termu);
			
			Set<Integer> intersection = new HashSet<Integer>(docsofw); // use the copy constructor
			intersection.retainAll(docsofu);
			
			double cooccurence_freq_w_u = (double)intersection.size();
			LexiconEntry leu = lu.getValue();
			double p_Xu_1 = (double)leu.getDocumentFrequency()/N;
			double p_Xu_0 = 1.0-p_Xu_1;
			double p_Xw_1_Xu_1 = cooccurence_freq_w_u/N; //(double)this.cooccurencemap.get_w_u(w, u)/N; //these are the same, just the first option is cheaper
			double p_Xw_1_Xu_0 = (double)(lew.getDocumentFrequency() - cooccurence_freq_w_u)/N;
			double p_Xw_0_Xu_1 = (double)(leu.getDocumentFrequency() - cooccurence_freq_w_u)/N;
			double p_Xw_0_Xu_0 = 1.0 - p_Xw_0_Xu_1 - p_Xw_1_Xu_0 - p_Xw_1_Xu_1;
			
			double I_w_u = p_Xw_0_Xu_0 * WeightingModelLibrary.log( p_Xw_0_Xu_0 / (p_Xw_0 * p_Xu_0) ) 
					+ p_Xw_1_Xu_0 * WeightingModelLibrary.log( p_Xw_1_Xu_0 / (p_Xw_1 * p_Xu_0) )
					+ p_Xw_0_Xu_1 * WeightingModelLibrary.log( p_Xw_0_Xu_1 / (p_Xw_0 * p_Xu_1) )
					+ p_Xw_1_Xu_1 * WeightingModelLibrary.log( p_Xw_1_Xu_1 / (p_Xw_1 * p_Xu_1) )
					;	
			
			double sum_all_w=0.0;
			Iterator<Entry <String, LexiconEntry> > itw = lex.iterator();
			//iterating over all possible w for the normalisation step
			while(itw.hasNext()) {
				Entry<String, LexiconEntry> lwprime = itw.next();
				if(lwprime.getValue().getFrequency()<this.rarethreshold || lwprime.getValue().getFrequency()>this.topthreshold)
					continue;
				String wprime = lwprime.getKey();
				LexiconEntry lewprime = lwprime.getValue();
				Set<Integer> docsofwprime = occursin_binary(wprime);
				Set<Integer> intersectionwprime = new HashSet<Integer>(docsofwprime);
				intersectionwprime.retainAll(docsofu);
				double cooccurence_freq_wprime_u = (double)intersectionwprime.size();
				double p_Xwprime_1_Xu_1 = cooccurence_freq_wprime_u/N; //(double)this.cooccurencemap.get_w_u(w, u)/N; //these are the same, just the first option is cheaper
				double p_Xwprime_1_Xu_0 = (double)(lewprime.getDocumentFrequency() - cooccurence_freq_wprime_u)/N;
				double p_Xwprime_0_Xu_1 = (double)(leu.getDocumentFrequency() - cooccurence_freq_wprime_u)/N;
				double p_Xwprime_0_Xu_0 = 1.0 - p_Xwprime_0_Xu_1 - p_Xwprime_1_Xu_0 - p_Xwprime_1_Xu_1;
				double p_Xwprime_1 = (double)lewprime.getDocumentFrequency()/N;
				double p_Xwprime_0 = 1.0-p_Xwprime_1;
				
				double I_wprime_u = p_Xwprime_0_Xu_0 * WeightingModelLibrary.log( p_Xwprime_0_Xu_0 / (p_Xwprime_0 * p_Xu_0) ) 
						+ p_Xwprime_1_Xu_0 * WeightingModelLibrary.log( p_Xwprime_1_Xu_0 / (p_Xwprime_1 * p_Xu_0) )
						+ p_Xwprime_0_Xu_1 * WeightingModelLibrary.log( p_Xwprime_0_Xu_1 / (p_Xwprime_0 * p_Xu_1) )
						+ p_Xwprime_1_Xu_1 * WeightingModelLibrary.log( p_Xwprime_1_Xu_1 / (p_Xwprime_1 * p_Xu_1) )
						;
				sum_all_w = sum_all_w + I_wprime_u;
			}
			
			w_cooccurence.put(termu, I_w_u/sum_all_w);
		}
		
		return w_cooccurence;
	}
	
	
	public static boolean isNumeric(String str)  
	{  
	  try  
	  {  
	    double d = Double.parseDouble(str);  
	  }  
	  catch(NumberFormatException nfe)  
	  {  
	    return false;  
	  }  
	  return true;  
	}
	
	HashMap<String, Double> getTopMiTranslations(String w) throws IOException {
		Lexicon<String> lex = index.getLexicon();
		double N = (double)index.getCollectionStatistics().getNumberOfDocuments();
		
		TreeMultimap<Double, String> inverted_translation_w = TreeMultimap.create(Ordering.natural().reverse(), Ordering.natural());
		
		Set<Integer> docsofw = occursin_binary(w);
		Iterator<Entry <String, LexiconEntry> > itu = lex.iterator();
		LexiconEntry lew = lex.getLexiconEntry(w);
		double p_Xw_1 = (double)lew.getDocumentFrequency()/N;
		double p_Xw_0 = 1.0-p_Xw_1;
		while(itu.hasNext()) {
			Entry<String, LexiconEntry> lu = itu.next();
			String termu = lu.getKey();
			
			if(lu.getValue().getFrequency()<this.rarethreshold || lu.getValue().getDocumentFrequency()<this.rarethreshold 
					|| lu.getValue().getDocumentFrequency()>this.topthreshold || termu.matches(".*\\d+.*"))
				continue;
			System.out.println("\tprocessing translation to " + termu + " (in " + lu.getValue().getDocumentFrequency() + " docs)");
			//System.out.println("\tmeasuring co-occurence with " + termu);
			//LexiconEntry leu = lu.getValue();
			Set<Integer> docsofu = occursin_binary(termu);
			
			Set<Integer> intersection = new HashSet<Integer>(docsofw); // use the copy constructor
			intersection.retainAll(docsofu);
			
			double cooccurence_freq_w_u = (double)intersection.size();
			LexiconEntry leu = lu.getValue();
			double p_Xu_1 = (double)leu.getDocumentFrequency()/N;
			double p_Xu_0 = 1.0-p_Xu_1;
			double p_Xw_1_Xu_1 = cooccurence_freq_w_u/N; //(double)this.cooccurencemap.get_w_u(w, u)/N; //these are the same, just the first option is cheaper
			double p_Xw_1_Xu_0 = (double)(lew.getDocumentFrequency() - cooccurence_freq_w_u)/N;
			double p_Xw_0_Xu_1 = (double)(leu.getDocumentFrequency() - cooccurence_freq_w_u)/N;
			double p_Xw_0_Xu_0 = 1.0 - p_Xw_0_Xu_1 - p_Xw_1_Xu_0 - p_Xw_1_Xu_1;
			
			double I_w_u = p_Xw_0_Xu_0 * WeightingModelLibrary.log( p_Xw_0_Xu_0 / (p_Xw_0 * p_Xu_0) ) 
					+ p_Xw_1_Xu_0 * WeightingModelLibrary.log( p_Xw_1_Xu_0 / (p_Xw_1 * p_Xu_0) )
					+ p_Xw_0_Xu_1 * WeightingModelLibrary.log( p_Xw_0_Xu_1 / (p_Xw_0 * p_Xu_1) )
					+ p_Xw_1_Xu_1 * WeightingModelLibrary.log( p_Xw_1_Xu_1 / (p_Xw_1 * p_Xu_1) )
					;	
			
			double sum_all_w=0.0;
			Iterator<Entry <String, LexiconEntry> > itw = lex.iterator();
			System.out.println("\tnow normalising " + termu);
			//iterating over all possible w for the normalisation step
			while(itw.hasNext()) {
				Entry<String, LexiconEntry> lwprime = itw.next();
				String wprime = lwprime.getKey();
				if(lwprime.getValue().getFrequency()<this.rarethreshold || lwprime.getValue().getDocumentFrequency()<this.rarethreshold 
						|| lwprime.getValue().getDocumentFrequency()>this.topthreshold || wprime.matches(".*\\d+.*"))
					continue;
				LexiconEntry lewprime = lwprime.getValue();
				Set<Integer> docsofwprime = occursin_binary(wprime);
				Set<Integer> intersectionwprime = new HashSet<Integer>(docsofwprime);
				intersectionwprime.retainAll(docsofu);
				double cooccurence_freq_wprime_u = (double)intersectionwprime.size();
				double p_Xwprime_1_Xu_1 = cooccurence_freq_wprime_u/N; //(double)this.cooccurencemap.get_w_u(w, u)/N; //these are the same, just the first option is cheaper
				double p_Xwprime_1_Xu_0 = (double)(lewprime.getDocumentFrequency() - cooccurence_freq_wprime_u)/N;
				double p_Xwprime_0_Xu_1 = (double)(leu.getDocumentFrequency() - cooccurence_freq_wprime_u)/N;
				double p_Xwprime_0_Xu_0 = 1.0 - p_Xwprime_0_Xu_1 - p_Xwprime_1_Xu_0 - p_Xwprime_1_Xu_1;
				double p_Xwprime_1 = (double)lewprime.getDocumentFrequency()/N;
				double p_Xwprime_0 = 1.0-p_Xwprime_1;
				
				double I_wprime_u = p_Xwprime_0_Xu_0 * WeightingModelLibrary.log( p_Xwprime_0_Xu_0 / (p_Xwprime_0 * p_Xu_0) ) 
						+ p_Xwprime_1_Xu_0 * WeightingModelLibrary.log( p_Xwprime_1_Xu_0 / (p_Xwprime_1 * p_Xu_0) )
						+ p_Xwprime_0_Xu_1 * WeightingModelLibrary.log( p_Xwprime_0_Xu_1 / (p_Xwprime_0 * p_Xu_1) )
						+ p_Xwprime_1_Xu_1 * WeightingModelLibrary.log( p_Xwprime_1_Xu_1 / (p_Xwprime_1 * p_Xu_1) )
						;
				sum_all_w = sum_all_w + I_wprime_u;
			}
			
			inverted_translation_w.put(I_w_u/sum_all_w, termu);
		}
		System.out.println("Selecting top terms");
		
		
		//do the selection of top terms from the inverted_translation_w and return them
		HashMap<String, Double> w_top_cooccurence = new HashMap<String, Double>();
		System.out.println("/tTranslations for " + w);
		int count =0;
		for (Double p_w_u : inverted_translation_w.keySet()) {
			if(count<this.number_of_top_translation_terms) {
				NavigableSet<String> terms = inverted_translation_w.get(p_w_u);
				Iterator<String> termit = terms.iterator();
				while(termit.hasNext()) {
					String topterm = termit.next();
					if(count<this.number_of_top_translation_terms) {
						System.out.println("\t  " + p_w_u + ": " + topterm);
						w_top_cooccurence.put(topterm, p_w_u);
						count ++;
					}else
						break;
				}
			}else
				break;
		}
		return w_top_cooccurence;
	}
	
	/*
	 * This implement the CMI/s method from the ECIR paper from Zhai.
	 * It uses the value of parameter alpha as the value of s for convenience
	 * */
	HashMap<String, Double> getTopMiTranslations_unormalisation_smethod(String w) throws IOException, InterruptedException {
		//Lexicon<String> lex = index.getLexicon();
		
		LexiconEntry lEntry = this.lex.getLexiconEntry(w);
		if(lEntry.getFrequency()<this.rarethreshold || lEntry.getDocumentFrequency()<this.rarethreshold 
				|| lEntry.getDocumentFrequency()>this.topthreshold || w.matches(".*\\d+.*")) 
		{
			System.err.println("No translations recorded for term " + w);
			HashMap<String, Double> w_top_cooccurence = new HashMap<String, Double>();
			w_top_cooccurence.put(w, 1.0);
			return w_top_cooccurence;
		}
		
		double N = (double)index.getCollectionStatistics().getNumberOfDocuments();
		
		TreeMultimap<Double, String> inverted_translation_w = TreeMultimap.create(Ordering.natural().reverse(), Ordering.natural());
		
		Set<Integer> docsofw = occursin_binary(w);
		Iterator<Entry <String, LexiconEntry> > itu = lex.iterator();
		LexiconEntry lew = lex.getLexiconEntry(w);
		double p_Xw_1 = (double)lew.getDocumentFrequency()/N;
		double p_Xw_0 = 1.0-p_Xw_1;
		double sum_all_u=0.0; //for normalisation
		HashMap<String, Double> tmp_u_container = new HashMap<String, Double>();
		while(itu.hasNext()) {
			Entry<String, LexiconEntry> lu = itu.next();
			String termu = lu.getKey();
			
			if(lu.getValue().getFrequency()<this.rarethreshold || lu.getValue().getDocumentFrequency()<this.rarethreshold 
					|| lu.getValue().getDocumentFrequency()>this.topthreshold || termu.matches(".*\\d+.*"))
				continue;
			//System.out.println("\tprocessing translation to " + termu + " (in " + lu.getValue().getDocumentFrequency() + " docs)");
			//System.out.println("\tmeasuring co-occurence with " + termu);
			//LexiconEntry leu = lu.getValue();
			Set<Integer> docsofu = occursin_binary(termu);
			
			Set<Integer> intersection = new HashSet<Integer>(docsofw); // use the copy constructor
			intersection.retainAll(docsofu);
			
			double cooccurence_freq_w_u = (double)intersection.size();
			LexiconEntry leu = lu.getValue();
			double p_Xu_1 = (double)leu.getDocumentFrequency()/N;
			double p_Xu_0 = 1.0-p_Xu_1;
			double p_Xw_1_Xu_1 = cooccurence_freq_w_u/N; //(double)this.cooccurencemap.get_w_u(w, u)/N; //these are the same, just the first option is cheaper
			double p_Xw_1_Xu_0 = (double)(lew.getDocumentFrequency() - cooccurence_freq_w_u)/N;
			double p_Xw_0_Xu_1 = (double)(leu.getDocumentFrequency() - cooccurence_freq_w_u)/N;
			double p_Xw_0_Xu_0 = 1.0 - p_Xw_0_Xu_1 - p_Xw_1_Xu_0 - p_Xw_1_Xu_1;
			
			double p00 = p_Xw_0_Xu_0 * WeightingModelLibrary.log( p_Xw_0_Xu_0 / (p_Xw_0 * p_Xu_0) );
			double p10 = p_Xw_1_Xu_0 * WeightingModelLibrary.log( p_Xw_1_Xu_0 / (p_Xw_1 * p_Xu_0) );
			double p01 = p_Xw_0_Xu_1 * WeightingModelLibrary.log( p_Xw_0_Xu_1 / (p_Xw_0 * p_Xu_1) );
			double p11 = p_Xw_1_Xu_1 * WeightingModelLibrary.log( p_Xw_1_Xu_1 / (p_Xw_1 * p_Xu_1) );
			
			if(p_Xw_0_Xu_0==0)
				p00=0.0;
			if(p_Xw_1_Xu_0==0)
				p10=0.0;
			if(p_Xw_0_Xu_1==0)
				p01=0.0;
			if(p_Xw_1_Xu_1==0)
				p11=0.0;
			
			/*double I_w_u = p_Xw_0_Xu_0 * WeightingModelLibrary.log( p_Xw_0_Xu_0 / (p_Xw_0 * p_Xu_0) ) 
					+ p_Xw_1_Xu_0 * WeightingModelLibrary.log( p_Xw_1_Xu_0 / (p_Xw_1 * p_Xu_0) )
					+ p_Xw_0_Xu_1 * WeightingModelLibrary.log( p_Xw_0_Xu_1 / (p_Xw_0 * p_Xu_1) )
					+ p_Xw_1_Xu_1 * WeightingModelLibrary.log( p_Xw_1_Xu_1 / (p_Xw_1 * p_Xu_1) )
					;	*/
			double I_w_u = p00 + p10 + p01 + p11;

			
			if(Double.isNaN(I_w_u)) {
				System.err.println("Problems occures when processing translation to " + termu + " (in " + lu.getValue().getDocumentFrequency() + " docs");
			}
			sum_all_u = sum_all_u + I_w_u;
			tmp_u_container.put(termu, I_w_u);
		}
		for(String u : tmp_u_container.keySet()) {
			double I_w_u = tmp_u_container.get(u);
			/*if(u.equals(w))
				I_w_u = this.alpha;
			else {
				I_w_u = (1.0 - this.alpha) * I_w_u/(sum_all_u - tmp_u_container.get(w));
			}
			inverted_translation_w.put(I_w_u, u);*/
			tmp_u_container.put(u, I_w_u);
		}
		
		System.out.println("Selecting top " + this.number_of_top_translation_terms + " terms");
		
		
		//do the selection of top terms from the inverted_translation_w and return them
		HashMap<String, Double> w_top_cooccurence = new HashMap<String, Double>();
		System.out.println("\tTranslations for " + w);
		int count =0;
		double sum_top=0.0;
		for (Double p_w_u : inverted_translation_w.keySet()) {
			if(count<this.number_of_top_translation_terms) {
				NavigableSet<String> terms = inverted_translation_w.get(p_w_u);
				Iterator<String> termit = terms.iterator();
				while(termit.hasNext()) {
					String topterm = termit.next();
					if(count<this.number_of_top_translation_terms) {
						System.out.println("\t  " + p_w_u + ": " + topterm);
						w_top_cooccurence.put(topterm, p_w_u);
						//this is the local top normalisation step
						sum_top = sum_top + p_w_u;
						count ++;
					}else
						break;
				}
			}else
				break;
		}
		
		/*now apply the s method to rescale probabilities - 
		note that the sum normalisation is limited to the possible translations and is done on translation terms
		not on the whole vocabulary of source terms. This is for computational efficiency.
		*/
		for(String u : w_top_cooccurence.keySet()) {
			double I_w_u = w_top_cooccurence.get(u);
			if(u.equals(w))
				I_w_u = this.alpha;
			else {
				I_w_u = (1.0 - this.alpha) * I_w_u/(sum_top - w_top_cooccurence.get(u));
			}
			w_top_cooccurence.put(u, I_w_u);
		}
		
		return w_top_cooccurence;
	}
	
	
	
	/*
	 * This implement the CMI/s method from the ECIR paper from Zhai.
	 * It uses the value of parameter alpha as the value of s for convenience
	 * */
	HashMap<String, Double> getTopMiTranslations_unormalisation_alpha(String w) throws IOException, InterruptedException {
		//Lexicon<String> lex = index.getLexicon();
		
		LexiconEntry lEntry = this.lex.getLexiconEntry(w);
		if(lEntry.getFrequency()<this.rarethreshold || lEntry.getDocumentFrequency()<this.rarethreshold 
				|| lEntry.getDocumentFrequency()>this.topthreshold || w.matches(".*\\d+.*")) 
		{
			System.err.println("No translations recorded for term " + w);
			HashMap<String, Double> w_top_cooccurence = new HashMap<String, Double>();
			w_top_cooccurence.put(w, 1.0);
			return w_top_cooccurence;
		}
		
		double N = (double)index.getCollectionStatistics().getNumberOfDocuments();
		
		TreeMultimap<Double, String> inverted_translation_w = TreeMultimap.create(Ordering.natural().reverse(), Ordering.natural());
		
		Set<Integer> docsofw = occursin_binary(w);
		Iterator<Entry <String, LexiconEntry> > itu = lex.iterator();
		LexiconEntry lew = lex.getLexiconEntry(w);
		double p_Xw_1 = (double)lew.getDocumentFrequency()/N;
		double p_Xw_0 = 1.0-p_Xw_1;
		double sum_all_u=0.0; //for normalisation
		HashMap<String, Double> tmp_u_container = new HashMap<String, Double>();
		while(itu.hasNext()) {
			Entry<String, LexiconEntry> lu = itu.next();
			String termu = lu.getKey();
			
			if(lu.getValue().getFrequency()<this.rarethreshold || lu.getValue().getDocumentFrequency()<this.rarethreshold 
					|| lu.getValue().getDocumentFrequency()>this.topthreshold || termu.matches(".*\\d+.*"))
				continue;
			//System.out.println("\tprocessing translation to " + termu + " (in " + lu.getValue().getDocumentFrequency() + " docs)");
			//System.out.println("\tmeasuring co-occurence with " + termu);
			//LexiconEntry leu = lu.getValue();
			Set<Integer> docsofu = occursin_binary(termu);
			
			Set<Integer> intersection = new HashSet<Integer>(docsofw); // use the copy constructor
			intersection.retainAll(docsofu);
			
			double cooccurence_freq_w_u = (double)intersection.size();
			LexiconEntry leu = lu.getValue();
			double p_Xu_1 = (double)leu.getDocumentFrequency()/N;
			double p_Xu_0 = 1.0-p_Xu_1;
			double p_Xw_1_Xu_1 = cooccurence_freq_w_u/N; //(double)this.cooccurencemap.get_w_u(w, u)/N; //these are the same, just the first option is cheaper
			double p_Xw_1_Xu_0 = (double)(lew.getDocumentFrequency() - cooccurence_freq_w_u)/N;
			double p_Xw_0_Xu_1 = (double)(leu.getDocumentFrequency() - cooccurence_freq_w_u)/N;
			double p_Xw_0_Xu_0 = 1.0 - p_Xw_0_Xu_1 - p_Xw_1_Xu_0 - p_Xw_1_Xu_1;
			
			double p00 = p_Xw_0_Xu_0 * WeightingModelLibrary.log( p_Xw_0_Xu_0 / (p_Xw_0 * p_Xu_0) );
			double p10 = p_Xw_1_Xu_0 * WeightingModelLibrary.log( p_Xw_1_Xu_0 / (p_Xw_1 * p_Xu_0) );
			double p01 = p_Xw_0_Xu_1 * WeightingModelLibrary.log( p_Xw_0_Xu_1 / (p_Xw_0 * p_Xu_1) );
			double p11 = p_Xw_1_Xu_1 * WeightingModelLibrary.log( p_Xw_1_Xu_1 / (p_Xw_1 * p_Xu_1) );
			
			if(p_Xw_0_Xu_0==0)
				p00=0.0;
			if(p_Xw_1_Xu_0==0)
				p10=0.0;
			if(p_Xw_0_Xu_1==0)
				p01=0.0;
			if(p_Xw_1_Xu_1==0)
				p11=0.0;
			
			/*double I_w_u = p_Xw_0_Xu_0 * WeightingModelLibrary.log( p_Xw_0_Xu_0 / (p_Xw_0 * p_Xu_0) ) 
					+ p_Xw_1_Xu_0 * WeightingModelLibrary.log( p_Xw_1_Xu_0 / (p_Xw_1 * p_Xu_0) )
					+ p_Xw_0_Xu_1 * WeightingModelLibrary.log( p_Xw_0_Xu_1 / (p_Xw_0 * p_Xu_1) )
					+ p_Xw_1_Xu_1 * WeightingModelLibrary.log( p_Xw_1_Xu_1 / (p_Xw_1 * p_Xu_1) )
					;	*/
			double I_w_u = p00 + p10 + p01 + p11;

			
			if(Double.isNaN(I_w_u)) {
				System.err.println("Problems occures when processing translation to " + termu + " (in " + lu.getValue().getDocumentFrequency() + " docs");
			}
			sum_all_u = sum_all_u + I_w_u;
			tmp_u_container.put(termu, I_w_u);
		}
		for(String u : tmp_u_container.keySet()) {
			double I_w_u = tmp_u_container.get(u);
			/*if(u.equals(w))
				I_w_u = this.alpha;
			else {
				I_w_u = (1.0 - this.alpha) * I_w_u/(sum_all_u - tmp_u_container.get(w));
			}
			inverted_translation_w.put(I_w_u, u);*/
			tmp_u_container.put(u, I_w_u);
		}
		
		System.out.println("Selecting top " + this.number_of_top_translation_terms + " terms");
		
		
		//do the selection of top terms from the inverted_translation_w and return them
		HashMap<String, Double> w_top_cooccurence = new HashMap<String, Double>();
		System.out.println("\tTranslations for " + w);
		int count =0;
		double sum_top=0.0;
		for (Double p_w_u : inverted_translation_w.keySet()) {
			if(count<this.number_of_top_translation_terms) {
				NavigableSet<String> terms = inverted_translation_w.get(p_w_u);
				Iterator<String> termit = terms.iterator();
				while(termit.hasNext()) {
					String topterm = termit.next();
					if(count<this.number_of_top_translation_terms) {
						System.out.println("\t  " + p_w_u + ": " + topterm);
						w_top_cooccurence.put(topterm, p_w_u);
						//this is the local top normalisation step
						sum_top = sum_top + p_w_u;
						count ++;
					}else
						break;
				}
			}else
				break;
		}
		
		/*now apply the s method to rescale probabilities - 
		note that the sum normalisation is limited to the possible translations and is done on translation terms
		not on the whole vocabulary of source terms. This is for computational efficiency.
		*/
		for(String u : w_top_cooccurence.keySet()) {
			double I_w_u = w_top_cooccurence.get(u);
			if(u.equals(w))
				I_w_u = this.alpha + (1.0 - this.alpha) * w_top_cooccurence.get(u);
			else {
				I_w_u = (1.0 - this.alpha) * w_top_cooccurence.get(u);
			}
			w_top_cooccurence.put(u, I_w_u);
		}
		
		return w_top_cooccurence;
	}
	
	
	
	HashMap<String, Double> getTopMiTranslations_unormalisation(String w) throws IOException, InterruptedException {
		//Lexicon<String> lex = index.getLexicon();
		
		LexiconEntry lEntry = this.lex.getLexiconEntry(w);
		if(lEntry.getFrequency()<this.rarethreshold || lEntry.getDocumentFrequency()<this.rarethreshold 
				|| lEntry.getDocumentFrequency()>this.topthreshold || w.matches(".*\\d+.*")) 
		{
			System.err.println("No translations recorded for term " + w);
			HashMap<String, Double> w_top_cooccurence = new HashMap<String, Double>();
			w_top_cooccurence.put(w, 1.0);
			return w_top_cooccurence;
		}
		
		double N = (double)index.getCollectionStatistics().getNumberOfDocuments();
		
		TreeMultimap<Double, String> inverted_translation_w = TreeMultimap.create(Ordering.natural().reverse(), Ordering.natural());
		
		Set<Integer> docsofw = occursin_binary(w);
		Iterator<Entry <String, LexiconEntry> > itu = lex.iterator();
		LexiconEntry lew = lex.getLexiconEntry(w);
		double p_Xw_1 = (double)lew.getDocumentFrequency()/N;
		double p_Xw_0 = 1.0-p_Xw_1;
		double sum_all_u=0.0; //for normalisation
		HashMap<String, Double> tmp_u_container = new HashMap<String, Double>();
		while(itu.hasNext()) {
			Entry<String, LexiconEntry> lu = itu.next();
			String termu = lu.getKey();
			
			if(lu.getValue().getFrequency()<this.rarethreshold || lu.getValue().getDocumentFrequency()<this.rarethreshold 
					|| lu.getValue().getDocumentFrequency()>this.topthreshold || termu.matches(".*\\d+.*"))
				continue;
			//System.out.println("\tprocessing translation to " + termu + " (in " + lu.getValue().getDocumentFrequency() + " docs)");
			//System.out.println("\tmeasuring co-occurence with " + termu);
			//LexiconEntry leu = lu.getValue();
			Set<Integer> docsofu = occursin_binary(termu);
			
			Set<Integer> intersection = new HashSet<Integer>(docsofw); // use the copy constructor
			intersection.retainAll(docsofu);
			
			double cooccurence_freq_w_u = (double)intersection.size();
			LexiconEntry leu = lu.getValue();
			double p_Xu_1 = (double)leu.getDocumentFrequency()/N;
			double p_Xu_0 = 1.0-p_Xu_1;
			double p_Xw_1_Xu_1 = cooccurence_freq_w_u/N; //(double)this.cooccurencemap.get_w_u(w, u)/N; //these are the same, just the first option is cheaper
			double p_Xw_1_Xu_0 = (double)(lew.getDocumentFrequency() - cooccurence_freq_w_u)/N;
			double p_Xw_0_Xu_1 = (double)(leu.getDocumentFrequency() - cooccurence_freq_w_u)/N;
			double p_Xw_0_Xu_0 = 1.0 - p_Xw_0_Xu_1 - p_Xw_1_Xu_0 - p_Xw_1_Xu_1;
			
			double p00 = p_Xw_0_Xu_0 * WeightingModelLibrary.log( p_Xw_0_Xu_0 / (p_Xw_0 * p_Xu_0) );
			double p10 = p_Xw_1_Xu_0 * WeightingModelLibrary.log( p_Xw_1_Xu_0 / (p_Xw_1 * p_Xu_0) );
			double p01 = p_Xw_0_Xu_1 * WeightingModelLibrary.log( p_Xw_0_Xu_1 / (p_Xw_0 * p_Xu_1) );
			double p11 = p_Xw_1_Xu_1 * WeightingModelLibrary.log( p_Xw_1_Xu_1 / (p_Xw_1 * p_Xu_1) );
			
			if(p_Xw_0_Xu_0==0)
				p00=0.0;
			if(p_Xw_1_Xu_0==0)
				p10=0.0;
			if(p_Xw_0_Xu_1==0)
				p01=0.0;
			if(p_Xw_1_Xu_1==0)
				p11=0.0;
			
			/*double I_w_u = p_Xw_0_Xu_0 * WeightingModelLibrary.log( p_Xw_0_Xu_0 / (p_Xw_0 * p_Xu_0) ) 
					+ p_Xw_1_Xu_0 * WeightingModelLibrary.log( p_Xw_1_Xu_0 / (p_Xw_1 * p_Xu_0) )
					+ p_Xw_0_Xu_1 * WeightingModelLibrary.log( p_Xw_0_Xu_1 / (p_Xw_0 * p_Xu_1) )
					+ p_Xw_1_Xu_1 * WeightingModelLibrary.log( p_Xw_1_Xu_1 / (p_Xw_1 * p_Xu_1) )
					;	*/
			double I_w_u = p00 + p10 + p01 + p11;

			
			if(Double.isNaN(I_w_u)) {
				System.out.println("Problems occures when processing translation to " + termu + " (in " + lu.getValue().getDocumentFrequency() + " docs");
				System.out.println("I_w_u=" + I_w_u + "\tsum_all_u=" + sum_all_u );
				System.out.println("p_Xu_1=" + p_Xu_1);
				System.out.println("p_Xu_0=" + p_Xu_0);
				System.out.println("p_Xw_1_Xu_1=" + p_Xw_1_Xu_1);
				System.out.println("p_Xw_1_Xu_0=" + p_Xw_1_Xu_0);
				System.out.println("lew.getDocumentFrequency()=" + lew.getDocumentFrequency() + "\tcooccurence_freq_w_u=" + cooccurence_freq_w_u);
				System.out.println("p_Xw_0_Xu_1=" + p_Xw_0_Xu_1);
				System.out.println("p_Xw_0_Xu_0=" + p_Xw_0_Xu_0);
				double test = p_Xw_0_Xu_0 * WeightingModelLibrary.log( p_Xw_0_Xu_0 / (p_Xw_0 * p_Xu_0) ) 
						+ p_Xw_1_Xu_0 * WeightingModelLibrary.log( p_Xw_1_Xu_0 / (p_Xw_1 * p_Xu_0) )
						+ p_Xw_0_Xu_1 * WeightingModelLibrary.log( p_Xw_0_Xu_1 / (p_Xw_0 * p_Xu_1) )
						+ p_Xw_1_Xu_1 * WeightingModelLibrary.log( p_Xw_1_Xu_1 / (p_Xw_1 * p_Xu_1) )
						;	
				System.out.println("p_Xw_0_Xu_0 * WeightingModelLibrary.log( p_Xw_0_Xu_0 / (p_Xw_0 * p_Xu_0) ): " 
						+ p_Xw_0_Xu_0 * WeightingModelLibrary.log( p_Xw_0_Xu_0 / (p_Xw_0 * p_Xu_0) ));
				
				System.out.println("p_Xw_1_Xu_0 * WeightingModelLibrary.log( p_Xw_1_Xu_0 / (p_Xw_1 * p_Xu_0) ): " 
						+ p_Xw_1_Xu_0 * WeightingModelLibrary.log( p_Xw_1_Xu_0 / (p_Xw_1 * p_Xu_0) ));
				System.out.println("p_Xw_1_Xu_0: " + p_Xw_1_Xu_0);
				System.out.println("p_Xw_1: " + p_Xw_1);
				System.out.println("p_Xu_0: " + p_Xu_0);
				
				
				System.out.println("p_Xw_0_Xu_1 * WeightingModelLibrary.log( p_Xw_0_Xu_1 / (p_Xw_0 * p_Xu_1) ): " 
						+ p_Xw_0_Xu_1 * WeightingModelLibrary.log( p_Xw_0_Xu_1 / (p_Xw_0 * p_Xu_1) ));
				
				System.out.println("p_Xw_1_Xu_1 * WeightingModelLibrary.log( p_Xw_1_Xu_1 / (p_Xw_1 * p_Xu_1) ): " 
						+ p_Xw_1_Xu_1 * WeightingModelLibrary.log( p_Xw_1_Xu_1 / (p_Xw_1 * p_Xu_1) ));
				Thread.sleep(6000);
			}
			sum_all_u = sum_all_u + I_w_u;
			tmp_u_container.put(termu, I_w_u);
		}
		for(String u : tmp_u_container.keySet()) {
			double I_w_u = tmp_u_container.get(u);
			//System.out.println("Normalisation step: I_w_u=" + I_w_u + "\t" + sum_all_u);
			inverted_translation_w.put(I_w_u/sum_all_u, u);
		}
		
		System.out.println("Selecting top " + this.number_of_top_translation_terms + " terms");
		
		
		//do the selection of top terms from the inverted_translation_w and return them
		HashMap<String, Double> w_top_cooccurence = new HashMap<String, Double>();
		System.out.println("\tTranslations for " + w);
		int count =0;
		double sum_top=0.0;
		for (Double p_w_u : inverted_translation_w.keySet()) {
			if(count<this.number_of_top_translation_terms) {
				NavigableSet<String> terms = inverted_translation_w.get(p_w_u);
				Iterator<String> termit = terms.iterator();
				while(termit.hasNext()) {
					String topterm = termit.next();
					if(count<this.number_of_top_translation_terms) {
						System.out.println("\t  " + p_w_u + ": " + topterm);
						w_top_cooccurence.put(topterm, p_w_u);
						//this is the local top normalisation step
						sum_top = sum_top + p_w_u;
						count ++;
					}else
						break;
				}
			}else
				break;
		}
		
		//now apply the local top normalisation step
		for(String u : w_top_cooccurence.keySet())
			w_top_cooccurence.put(u, w_top_cooccurence.get(u)/sum_top);
		
		
		return w_top_cooccurence;
	}
	
	
	
	
	
	
	public HashMap<String, Double> getTopW2VTranslations(String w) throws IOException {
		//do the selection of top terms from the inverted_translation_w and return them
		System.out.println("\tWord2Vec translations " + w);
		TreeMultimap<Double, String> inverted_translation_w = w2v_inverted_translation.get(w);
		HashMap<String, Double> w_top_cooccurence = new HashMap<String, Double>();
		if(!w2v_inverted_translation.containsKey(w)) {
			System.err.println("No translations recorded for term " + w);
			w_top_cooccurence.put(w, 1.0);
			return w_top_cooccurence;
		}
		System.out.println("\tTranslations for " + w);
		int count =0;

		double sums_u=0.0;
		for (Double p_w_u : inverted_translation_w.keySet()) {
			if(count<this.number_of_top_translation_terms) {
				NavigableSet<String> terms = inverted_translation_w.get(p_w_u);
				Iterator<String> termit = terms.iterator();
				while(termit.hasNext()) {
					String topterm = termit.next();
					if(count<this.number_of_top_translation_terms) {
						System.out.println("\t  " + p_w_u + ": " + topterm);
						w_top_cooccurence.put(topterm, p_w_u);
						sums_u=sums_u + p_w_u;
						count ++;
					}else
						break;
				}
			}else
				break;
		}
		
		//normalised based on u
		HashMap<String, Double> tmp_w_top_cooccurence = new HashMap<String, Double>();
		int tcount=0;
		double cumsum=0.0;
		for(String u: w_top_cooccurence.keySet()) {
			tmp_w_top_cooccurence.put(u, w_top_cooccurence.get(u)/sums_u);
			cumsum=cumsum+w_top_cooccurence.get(u)/sums_u;
			tcount++;
		}
		System.out.println(tcount + " translations selected, for a cumulative sum of " + cumsum);
		return tmp_w_top_cooccurence;
	}
	
	public HashMap<String, Double> getTopW2VTranslations_atquerytime(String w) {
		TreeMultimap<Double, String> inverted_translation_w = TreeMultimap.create(Ordering.natural().reverse(), Ordering.natural());
		HashMap<String, Double> w_top_cooccurence = new HashMap<String, Double>();
		LexiconEntry lEntry = this.lex.getLexiconEntry(w);
		if(lEntry.getFrequency()<this.rarethreshold || lEntry.getDocumentFrequency()<this.rarethreshold 
				|| lEntry.getDocumentFrequency()>this.topthreshold || w.matches(".*\\d+.*")) 
		{
			System.err.println("No translations recorded for term " + w);
			w_top_cooccurence.put(w, 1.0);
			return w_top_cooccurence;
		}

		if(!w2v_inverted_translation.containsKey(w)) {
			double[] vector_w = fullw2vmatrix.get(w);
			HashMap<String,Double> tmp_w = new HashMap<String,Double>();
			double sum_cosines=0.0;
			for(String u : fullw2vmatrix.keySet()) {
				double[] vector_u = fullw2vmatrix.get(u);
				double cosine_w_u=0.0;
				double sum_w=0.0;
				double sum_u=0.0;
				for(int i=0; i<vector_w.length;i++) {
					cosine_w_u=cosine_w_u + vector_w[i]*vector_u[i];
					sum_w=sum_w + Math.pow(vector_w[i],2);
					sum_u=sum_u + Math.pow(vector_u[i],2);
				}
				//System.out.println("Un-normalised cosine: " + cosine_w_u);
				//normalisation step
				cosine_w_u = cosine_w_u / (Math.sqrt(sum_w) * Math.sqrt(sum_u));
				//System.out.println("normalised cosine: " + cosine_w_u);
				tmp_w.put(u, cosine_w_u);
				sum_cosines = sum_cosines+ cosine_w_u;
			}
			//normalise to probabilities and insert in order
			for(String u: tmp_w.keySet()) {
				double p_w2v_w_u = tmp_w.get(u)/sum_cosines;
				inverted_translation_w.put(p_w2v_w_u, u);
			}
			w2v_inverted_translation.put(w, inverted_translation_w);
		}else {
			inverted_translation_w = w2v_inverted_translation.get(w);
			System.out.println("Translation already available in memory");
		}

		System.out.println("\tWord2Vec translations " + w);
		//TreeMultimap<Double, String> inverted_translation_w = w2v_inverted_translation.get(w);

		if(!w2v_inverted_translation.containsKey(w)) {
			System.err.println("No translations recorded for term " + w);
			w_top_cooccurence.put(w, 1.0);
			return w_top_cooccurence;
		}
		System.out.println("\tTranslations for " + w);
		int count =0;

		double sums_u=0.0;
		for (Double p_w_u : inverted_translation_w.keySet()) {
			if(count<this.number_of_top_translation_terms) {
				NavigableSet<String> terms = inverted_translation_w.get(p_w_u);
				Iterator<String> termit = terms.iterator();
				while(termit.hasNext()) {
					String topterm = termit.next();
					if(count<this.number_of_top_translation_terms) {
						w_top_cooccurence.put(topterm, p_w_u);
						sums_u=sums_u + p_w_u;
						count ++;
					}else
						break;
				}
			}else
				break;
		}

		//normalised based on u
		HashMap<String, Double> tmp_w_top_cooccurence = new HashMap<String, Double>();
		int tcount=0;
		double cumsum=0.0;
		for(String u: w_top_cooccurence.keySet()) {
			tmp_w_top_cooccurence.put(u, w_top_cooccurence.get(u)/sums_u);
			System.out.println("\t  " + w_top_cooccurence.get(u)/sums_u + ": " + u);
			cumsum=cumsum+w_top_cooccurence.get(u)/sums_u;
			tcount++;
		}
		System.out.println(tcount + " translations selected, for a cumulative sum of " + cumsum);
		return tmp_w_top_cooccurence;
	}
	
	
	
	/*No normalisation of cosine - but normalised over the selected set of u*/
	public HashMap<String, Double> getTopW2VTranslations_atquerytime_notnormalised(String w) {
		TreeMultimap<Double, String> inverted_translation_w = TreeMultimap.create(Ordering.natural().reverse(), Ordering.natural());
		HashMap<String, Double> w_top_cooccurence = new HashMap<String, Double>();
		LexiconEntry lEntry = this.lex.getLexiconEntry(w);
		if(lEntry.getFrequency()<this.rarethreshold || lEntry.getDocumentFrequency()<this.rarethreshold 
				|| lEntry.getDocumentFrequency()>this.topthreshold || w.matches(".*\\d+.*")) 
		{
			System.err.println("No translations recorded for term " + w);
			w_top_cooccurence.put(w, 1.0);
			return w_top_cooccurence;
		}

		if(!w2v_inverted_translation.containsKey(w)) {
			double[] vector_w = fullw2vmatrix.get(w);
			HashMap<String,Double> tmp_w = new HashMap<String,Double>();
			double sum_cosines=0.0;
			for(String u : fullw2vmatrix.keySet()) {
				double[] vector_u = fullw2vmatrix.get(u);
				double cosine_w_u=0.0;
				double sum_w=0.0;
				double sum_u=0.0;
				for(int i=0; i<vector_w.length;i++) {
					cosine_w_u=cosine_w_u + vector_w[i]*vector_u[i];
					sum_w=sum_w + Math.pow(vector_w[i],2);
					sum_u=sum_u + Math.pow(vector_u[i],2);
				}
				//System.out.println("Un-normalised cosine: " + cosine_w_u);
				//normalisation step
				cosine_w_u = cosine_w_u / (Math.sqrt(sum_w) * Math.sqrt(sum_u));
				//System.out.println("normalised cosine: " + cosine_w_u);
				//tmp_w.put(u, cosine_w_u);
				inverted_translation_w.put(cosine_w_u, u);
				//sum_cosines = sum_cosines+ cosine_w_u;
			}
			//normalise to probabilities and insert in order
			/*for(String u: tmp_w.keySet()) {
				double p_w2v_w_u = tmp_w.get(u)/sum_cosines;
				
			}*/
			w2v_inverted_translation.put(w, inverted_translation_w);
		}else {
			inverted_translation_w = w2v_inverted_translation.get(w);
			System.out.println("Translation already available in memory");
		}

		System.out.println("\tWord2Vec translations " + w);
		//TreeMultimap<Double, String> inverted_translation_w = w2v_inverted_translation.get(w);

		if(!w2v_inverted_translation.containsKey(w)) {
			System.err.println("No translations recorded for term " + w);
			w_top_cooccurence.put(w, 1.0);
			return w_top_cooccurence;
		}
		System.out.println("\tTranslations for " + w);
		int count =0;

		double sums_u=0.0;
		for (Double p_w_u : inverted_translation_w.keySet()) {
			if(count<this.number_of_top_translation_terms) {
				NavigableSet<String> terms = inverted_translation_w.get(p_w_u);
				Iterator<String> termit = terms.iterator();
				while(termit.hasNext()) {
					String topterm = termit.next();
					if(count<this.number_of_top_translation_terms) {
						w_top_cooccurence.put(topterm, p_w_u);
						sums_u=sums_u + p_w_u;
						count ++;
					}else
						break;
				}
			}else
				break;
		}

		//normalised based on u
		HashMap<String, Double> tmp_w_top_cooccurence = new HashMap<String, Double>();
		int tcount=0;
		double cumsum=0.0;
		for(String u: w_top_cooccurence.keySet()) {
			tmp_w_top_cooccurence.put(u, w_top_cooccurence.get(u)/sums_u);
			System.out.println("\t  " + w_top_cooccurence.get(u)/sums_u + ": " + u);
			cumsum=cumsum+w_top_cooccurence.get(u)/sums_u;
			tcount++;
		}
		System.out.println(tcount + " translations selected, for a cumulative sum of " + cumsum);
		return tmp_w_top_cooccurence;
	}
	
	
	public void compute_mi_window() throws FileNotFoundException, IOException{
		HashMap<String, HashMap<String, Double>> translations = new HashMap<String, HashMap<String, Double>>();
		
		File folder = new File(this.getPath_to_mi_window_folder());
		for (final File fileEntry : folder.listFiles()) {
	        if (!fileEntry.isDirectory() && fileEntry.getName().endsWith(".txt")) {
	           String termq = fileEntry.getName().replace(".txt", "").toLowerCase();
	           try (BufferedReader br = new BufferedReader(new FileReader(fileEntry))) {
	        	    String line;
	        	    while ((line = br.readLine()) != null) {
	        	       String[] words = line.split(" ");
	        	       int target=-1;
	        	       for(int i=0; i<words.length; i++) {
	        	    	   if(words[i].equalsIgnoreCase(termq)) {
	        	    		   target = i;
	        	    		   break;
	        	    	   }
	        	       }
	        	       if(target==-1)
	        	    	   System.err.println("There is a PROBLEM!!");
	        	       else {
	        	    	   int lowerbound = target - this.getWindowsize();
	        	    	   lowerbound = Math.max(lowerbound, 0);
	        	    	   int upperbound = target + this.getWindowsize();
	        	    	   upperbound = Math.min(upperbound, words.length-1);
	        	    	   HashMap<String, Double> translationq = new HashMap<String, Double>();
    	    			   if(translations.containsKey(termq))
    	    				   translationq = translations.get(termq);
	        	    	   for(int j=lowerbound; j<=upperbound; j++) {
	        	    		   if(j!=target) {
	        	    			   String termu = words[j];
	        	    			   double count=0.0;
	        	    			   if(translationq.containsKey(termu))
	        	    				   count = translationq.get(termu);
	        	    			   count = count + 1.0;
	        	    			   translationq.put(termu, count);
	        	    		   }
	        	    	   }
	        	    	   translations.put(termq, translationq);
	        	    	   
	        	    	   
	        	       }
	        	       
	        	    }
	        	}

	        } 
	    }
		this.window_translations = translations;
		return;
		
	}
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println("test");
	}

}

