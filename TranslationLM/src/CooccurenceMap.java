
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import org.terrier.structures.DocumentIndex;
import org.terrier.structures.Index;
import org.terrier.structures.Lexicon;
import org.terrier.structures.LexiconEntry;
import org.terrier.structures.PostingIndex;
import org.terrier.structures.postings.IterablePosting;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;

/**
 * 
 */

/**
 * @author zuccong
 *
 */
public class CooccurenceMap implements Serializable{

	
	HashMap<String, HashMap<String, Integer> > cooccurencemap = new HashMap<String, HashMap<String, Integer> >();
	public transient Index index = null;
	public transient PostingIndex inv = null;
	int rarethreshold=0;
	int topthreshold=100000000;
	
	public CooccurenceMap(){
		
	}
	
	public CooccurenceMap(String cooccurencemap_path) {
		this.cooccurencemap=readmap(cooccurencemap_path);
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

	public void set_index(String index_path, String index_prefix) {
		this.index = Index.createIndex(index_path, index_prefix);
		this.inv = index.getInvertedIndex();
	}
	
	double get_w_u(String w, String u) {
		if(this.cooccurencemap.containsKey(w)) {
			if(this.cooccurencemap.get(w).containsKey(u)) {
				return this.cooccurencemap.get(w).get(u);
			}else
				return 0; //TODO: could log a specific error message
		}else
			return 0; //TODO: could log a specific error message
	}
	
	
	boolean contains_w_u(String w, String u) {
		if(this.cooccurencemap.containsKey(w)) {
			if(this.cooccurencemap.get(w).containsKey(u)) {
				return true;
			}else
				return false; //TODO: could log a specific error message
		}else
			return false; //TODO: could log a specific error message
	}
	
	
	
	void put_w_u(String w, String u, Integer cooccurence){
		HashMap<String, Integer> u_distribution = new HashMap<String, Integer>();
		if(this.cooccurencemap.containsKey(w)) {
			u_distribution = this.cooccurencemap.get(w);
			this.cooccurencemap.remove(w);
		}
		if(u_distribution.containsKey(u)) {
			//System.err.println("A value has been already recorded for " + w + " | " + u);
			//System.err.println("Not Overriding");
			//this.translationmap.put(w, u_distribution);
			//u_distribution.put(u, p_w_u);
			//do nothing
		}else {
			u_distribution.put(u, cooccurence);
		}
		this.cooccurencemap.put(w, u_distribution);
	}
	
	//returns the documents were w occurs in
	HashMap<Integer,Integer> occursin(String v) throws IOException{
		HashMap<Integer,Integer> docsofv = new HashMap<Integer,Integer>();
		
		//MetaIndex meta = index.getMetaIndex();
		Lexicon<String> lex = index.getLexicon();
		LexiconEntry lev = lex.getLexiconEntry( v );
		IterablePosting postings = inv.getPostings(lev);
		while (postings.next() != IterablePosting.EOL) {
			docsofv.put(postings.getId(), postings.getFrequency());
		}
		return docsofv;
	}
	
	
	//returns the documents were w occurs in - binary version
	Set<Integer> occursin_binary(String v) throws IOException{
		Set<Integer> vecv = new HashSet<Integer>();
		Lexicon<String> lex = index.getLexicon();
		LexiconEntry le = lex.getLexiconEntry( v );
		IterablePosting postings = inv.getPostings( le);
		while (postings.next() != IterablePosting.EOL) {
			vecv.add(postings.getId());
		}
		return vecv;
	}
	
	
	/** Builds a CooccurenceMap by iterating over the vocabulary of the collection. It counts document co-occurence, i.e. it doesn't consider the frequency of two terms in a document. 
	 * Complexity: O(n^3) = O(d t^2) where n is the number of terms in the vocabulary
	 * Note: this currently goes out of heap space on DOTGOV with 5GB of RAM allocated to the JVM
	 */
	void build_full_cooccurencemap() throws IOException {

		Lexicon<String> lex = index.getLexicon();
		Iterator<Entry <String, LexiconEntry> > itw = lex.iterator();
		int prcount=1;
		//iterating over all possible w
		while(itw.hasNext()) {
			Entry<String, LexiconEntry> lw = itw.next();
			String termw = lw.getKey();
			if(lw.getValue().getFrequency()<this.rarethreshold || lw.getValue().getFrequency()>this.topthreshold)
				continue;
			
			if(prcount % 1000 == 0)
				System.out.println("Processing... " + 100.0*((double)prcount)/this.index.getCollectionStatistics().getNumberOfUniqueTerms() + "%");
			prcount++;
			
			//LexiconEntry lew = lw.getValue();
			//System.out.println("analysing " + termw);
			HashMap<String, Integer> w_cooccurence = new HashMap<String, Integer>();
			if(cooccurencemap.containsKey(termw)) {
				w_cooccurence = cooccurencemap.get(termw);
				cooccurencemap.remove(termw);
			}
			
			Set<Integer> docsofw = occursin_binary(termw);
			Iterator<Entry <String, LexiconEntry> > itu = lex.iterator();
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
				int count = intersection.size();
				if(w_cooccurence.containsKey(termu)) {
	        		count = count + w_cooccurence.get(termu);
	        		w_cooccurence.remove(termu);
	        	}
				w_cooccurence.put(termu, count);
	        	//System.out.println("\t\t"+termw + " " + termu + " = " + count);
	        	//System.out.println(docsofw.size() + " " + docsofu.size() + " " + diff.entriesInCommon());
	        	
	        	//The next bit of code instead does count frequencies
				/*
				if(docsofw.size() <= docsofu.size()) {
					 for (Integer docidw: docsofw.keySet())
					    {
					        if (docsofu.containsKey(docidw)) {
					           //then w and u co-occur
					        	Integer count = (Integer) Math.min(docsofw.get(docidw), docsofu.get(docidw));
					        	if(w_cooccurence.containsKey(termu)) {
					        		count = count + w_cooccurence.get(termu);
					        		w_cooccurence.remove(termu);
					        	}
					        	w_cooccurence.put(termu, count);
					        	System.out.println("\t\t"+termw + " " + termu + " = " + count);
					        }
					    }
				}else {
					for (Integer docidu: docsofu.keySet())
				    {
				        if (docsofw.containsKey(docidu)) {
					           //then w and u co-occur
					        	Integer count = (Integer) Math.min(docsofw.get(docidu), docsofu.get(docidu));
					        	if(w_cooccurence.containsKey(termu)) {
					        		count = count + w_cooccurence.get(termu);
					        		w_cooccurence.remove(termu);
					        	}
					        	w_cooccurence.put(termu, count);
					        	System.out.println("\t\t"+termw + " " + termu + " = " + count);
					        }
				    }
				}*/
				
			}
			
			cooccurencemap.put(termw, w_cooccurence);
			//System.out.println(termw + ": " + w_cooccurence);
		}
		
		
	}
	
	
	/** Builds a CooccurenceMap by iterating over the documents of the collection. It counts document co-occurence, i.e. it doesn't consider the frequency of two terms in a document.
	 * Complexity: O(d * t *t/2) = O(d t^2) where d is the number of documents in the collection and t is the average number of terms per documents.
	 * Note that t = avg doc len
	 */
	public void build_full_cooccurencemap_docversion() throws IOException {
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
				if(lee.getValue().getFrequency()<this.rarethreshold || lee.getValue().getFrequency()>this.topthreshold)
					continue;
				
				HashMap<String, Integer> w_cooccurence = new HashMap<String, Integer>();
				if(this.cooccurencemap.containsKey(termw)) {
					w_cooccurence = this.cooccurencemap.get(termw);
					this.cooccurencemap.remove(termw);
				}
				Iterator<String> it = seenterms.iterator();
			    while(it.hasNext()) {
			    	String termu = it.next();
			    	int count =1;
			    	if(w_cooccurence.containsKey(termu)) {
		        		count = count + w_cooccurence.get(termu);
		        		w_cooccurence.remove(termu);
		        	}
					w_cooccurence.put(termu, count);
					
					//System.out.println(termw + ": " + w_cooccurence);
			    	//and now I need to do the symmetric
					HashMap<String, Integer> u_cooccurence = new HashMap<String, Integer>();
					if(cooccurencemap.containsKey(termu)) {
						u_cooccurence = cooccurencemap.get(termu);
						cooccurencemap.remove(termu);
					}
					int countu=1;
					if(u_cooccurence.containsKey(termw)) {
		        		countu = countu + u_cooccurence.get(termw);
		        		u_cooccurence.remove(termw);
		        	}
					u_cooccurence.put(termw, count);
					cooccurencemap.put(termu, u_cooccurence);
					//System.out.println(termu + ": " + u_cooccurence);
			    }
			    
			    cooccurencemap.put(termw, w_cooccurence);
			    seenterms.add(termw); // I add only the termw that are within the thresholds
			}
		}
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

			oos.writeObject(this.cooccurencemap);
			oos.flush();
			oos.close();
			fos.close();
		}catch(Exception e){}
	}
	
	/** Reads a serialized object of type CooccurenceMap
	 * 
	 * @param filepath - the path of the file on disk containing the CooccurenceMap
	 * @return a CooccurenceMap containing the data on file
	 */
	public HashMap<String, HashMap<String, Integer> > readmap(String filepath){
		HashMap<String, HashMap<String, Integer> > mapInFile = new HashMap<String, HashMap<String, Integer> >();
		try{
			FileInputStream fis=new FileInputStream(filepath);
			ObjectInputStream ois=new ObjectInputStream(fis);
			mapInFile=(HashMap<String, HashMap<String, Integer> >)ois.readObject();
			ois.close();
			fis.close();
		}catch(Exception e){}
		return mapInFile;
	}
	
	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		CooccurenceMap coccmap = new CooccurenceMap();
		//System.setProperty("terrier.home", "/Users/zuccong/tools/terrier-4.0");
		
		System.setProperty("terrier.home", args[0]);
		
		
		//coccmap.set_index("/Users/zuccong/experiments/dotgov_stoplist/", "data");
		coccmap.set_index(args[1], "data");
		coccmap.setRarethreshold(500);
		coccmap.setTopthreshold(coccmap.index.getCollectionStatistics().getNumberOfDocuments()/1000);
		coccmap.build_full_cooccurencemap_docversion();
		//coccmap.build_full_cooccurencemap();
		//coccmap.writemap("/Users/zuccong/experiments/cooccurence_dotgov_stoplist.map");
		coccmap.writemap(args[2]);
		System.out.println("Size written" +coccmap.cooccurencemap.size());
		
		/*
		System.out.println("Reading map from file");
		//CooccurenceMap coccmapr = coccmap.readmap("/Users/zuccong/experiments/sigir2015_nlm/cooccurence_dotgov_stoplist");
		CooccurenceMap coccmapr = coccmap.readmap(args[2]);
		//coccmapr.set_index("/Users/zuccong/experiments/dotgov_stoplist/", "data");
		coccmapr.set_index(args[1], "data");
		
		System.out.println("Size read " + coccmapr.cooccurencemap.size());
		*/
		CooccurenceMap coccmapr = coccmap;
		
		//This is just a testing loop: will only examine the first 5 terms
		int count = 5;
		for (String w : coccmapr.cooccurencemap.keySet()) {
		    if(count>0) {
		    	count--;
				System.out.println(w);
				HashMap<String, Integer> w_cooccurence = coccmapr.cooccurencemap.get(w);
				for (String u : w_cooccurence.keySet()) {
					System.out.println("\t" + u + ": " + w_cooccurence.get(u));
					
					Set<Integer> vecw = new HashSet<Integer>();
					Lexicon<String> lex = coccmapr.index.getLexicon();
					LexiconEntry le = lex.getLexiconEntry( w );
					IterablePosting postings = coccmapr.inv.getPostings( le);
					while (postings.next() != IterablePosting.EOL) {
						vecw.add(postings.getId());
					}
					
					Set<Integer> vecu = new HashSet<Integer>();
					LexiconEntry leu = lex.getLexiconEntry( u );
					IterablePosting postingsu = coccmapr.inv.getPostings( leu);
					while (postingsu.next() != IterablePosting.EOL) {
						vecu.add(postingsu.getId());
					}
					Set<Integer> intersection = new HashSet<Integer>(vecw); // use the copy constructor
					intersection.retainAll(vecu);
					System.out.println("\tintersection: " + intersection.size() + " size w: " + vecw.size() + " size u: " + vecu.size());
					
				}
		    }
		}

		
		System.out.println("co-occurrence(fracture,doctor) = " + coccmap.get_w_u("holiday", "meeting"));
		System.out.println("co-occurrence(doctor,fracture) = " + coccmap.get_w_u("meeting", "holiday"));
		
		System.out.println("co-occurrence(risk,economy) = " + coccmap.get_w_u("risk", "economy"));
		System.out.println("co-occurrence(economy,risk) = " + coccmap.get_w_u("economy", "risk"));
		
		System.out.println("co-occurrence(dollar,million) = " + coccmap.get_w_u("dollar", "million"));
		System.out.println("co-occurrence(million,dollar) = " + coccmap.get_w_u("million", "dollar"));
		
	}

}
