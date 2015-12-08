import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;

/**
 * 
 */

/**
 * @author zuccong
 *
 *	This class is a utility class. It is a datastructure to keep distributions of translations and their reverse distribution
 *
 * <p>Considerations on time complexity
 *
 * From the Java HashMap documentation:
 * <p>An instance of <tt>HashMap</tt> has two parameters that affect its
 * performance: <i>initial capacity</i> and <i>load factor</i>. The
 * <i>capacity</i> is the number of buckets in the hash table, and the initial
 * capacity is simply the capacity at the time the hash table is created. The
 * <i>load factor</i> is a measure of how full the hash table is allowed to
 * get before its capacity is automatically increased. When the number of
 * entries in the hash table exceeds the product of the load factor and the
 * current capacity, the capacity is roughly doubled by calling the
 * <tt>rehash</tt> method.
 *
 * <p>As a general rule, the default load factor (.75) offers a good tradeoff
 * between time and space costs. Higher values decrease the space overhead
 * but increase the lookup cost (reflected in most of the operations of the
 * <tt>HashMap</tt> class, including <tt>get</tt> and <tt>put</tt>). The
 * expected number of entries in the map and its load factor should be taken
 * into account when setting its initial capacity, so as to minimize the
 * number of <tt>rehash</tt> operations. If the initial capacity is greater
 * than the maximum number of entries divided by the load factor, no
 * <tt>rehash</tt> operations will ever occur.
 *
 *
 */
public class TranslationMap implements Serializable{
	

	//HashMap<String, Double> p_w_u_distribution = new HashMap<String, Double>();
	HashMap<String, HashMap<String, Double> > translationmap = new HashMap<String, HashMap<String, Double> >();
	//Multimap<Double, String> p_w_u_distribution_inverted = TreeMultimap.create();  
	HashMap<String, TreeMultimap<Double, String> > inverted_translationmap = new HashMap<String, TreeMultimap<Double, String> >();
	//TreeMultimap<Double,String> p_w_u_distribution_inverted = new TreeMultimap<Double,String>(Ordering.natural().reverse(), Ordering.natural());
	
	public TranslationMap() throws IOException{
		this.translationmap = new HashMap<String, HashMap<String, Double> >();
		this.inverted_translationmap = new HashMap<String, TreeMultimap<Double, String> >();
	}
	
	
	public double get_w_u(String w, String u) {
		if(this.translationmap.containsKey(w)) {
			if(this.translationmap.get(w).containsKey(u)) {
				return this.translationmap.get(w).get(u);
			}else
				return 0.0; //TODO: could log a specific error message
		}else
			return 0.0; //TODO: could log a specific error message
	}
	
	
	public boolean contains_w_u(String w, String u) {
		if(this.translationmap.containsKey(w)) {
			if(this.translationmap.get(w).containsKey(u)) {
				return true;
			}else
				return false; //TODO: could log a specific error message
		}else
			return false; //TODO: could log a specific error message
	}
	
	public HashMap<String,Double> get_topu_givenw(String w, int top){
		int i=0;
		TreeMultimap<Double, String> p_w_u_distribution_inverted = this.inverted_translationmap.get(w);
		HashMap<String,Double> top_p_w_u_distribution = new HashMap<String,Double>();
		
		//this returns the top terms: if two terms have the same score and they are at the boundary of top, then the one 
		//that comes first alphabetically is returned (break ties)
		/*Set<Map.Entry<Double,String>> pairs = p_w_u_distribution_inverted.entries();
		for(Entry<Double, String>  e : pairs) {
			if(i<top) {
				i++;
				System.out.println(e.getKey()+": "+e.getValue());
				String u = e.getValue();
				top_p_w_u_distribution.put(u, e.getKey());
			}
		}*/

		//this returns the top terms with no break of ties
		for(Entry<Double, Collection<String>>  e : p_w_u_distribution_inverted.asMap().entrySet()) {
			if(i<top) {
				i++;
				System.out.println(e.getKey()+": "+e.getValue());
				  Collection<String> us = e.getValue();
				  for(String u : us) {
					  top_p_w_u_distribution.put(u, e.getKey());
				  }
			}
		}
		return top_p_w_u_distribution;
	}
	
	
	/*boolean put_p_w_u(String w, String u, Double p_w_u){
		if(this.contains_w_u(w, u)) {
			System.err.println("A value has been already recorded for " + w + " | " + u);
			System.err.println("Not Overriding");
			//HashMap<String, Double> u_distribution = this.translationmap.get(w);
			//u_distribution.put(u, p_w_u);
			//return true;
			return false;
		}else {
			HashMap<String, Double> u_distribution = new HashMap<String, Double>();
			if(this.translationmap.containsKey(w)) {
				u_distribution = this.translationmap.get(w);
				this.translationmap.remove(w);
			}
			if(u_distribution.containsKey(u)) {
				System.err.println("A value has been already recorded for " + w + " | " + u);
				System.err.println("Not Overriding");
				this.translationmap.put(w, u_distribution);
				//u_distribution.put(u, p_w_u);
				return false;
			}else {
				u_distribution.put(u, p_w_u);
			}
			this.translationmap.put(w, u_distribution);

			//now taking care of the inverse distribution
			if(this.inverted_translationmap.containsKey(w)) {
				Multimap<Double, String> u_inverted_distribution = this.inverted_translationmap.get(w);
				if(u_inverted_distribution.containsEntry(p_w_u, u)) {
					System.err.println("A value has been already recorded for " + w + " | " + u + " in the inverse distribution");
					return false;
				}
				Collection<String> p_w_u_distribution = new ArrayList<String>();
				if(u_inverted_distribution.containsKey(p_w_u)) {
					p_w_u_distribution = u_inverted_distribution.get(p_w_u);
					u_inverted_distribution.removeAll(p_w_u);
				}
				if(p_w_u_distribution.contains(u)) {
					System.err.println("A value has been already recorded for " + w + " | " + u + " in the inverse distribution");
					u_inverted_distribution.putAll(p_w_u, p_w_u_distribution);
					return false;
				}else {
					p_w_u_distribution.add(u);
					u_inverted_distribution.putAll(p_w_u, p_w_u_distribution);
				}
				this.inverted_translationmap.remove(w);
				this.inverted_translationmap.put(w, u_inverted_distribution);
			}else {
				Multimap<Double, String> u_inverted_distribution = ArrayListMultimap.create();
				u_inverted_distribution.put(p_w_u, u);
				this.inverted_translationmap.put(w, u_inverted_distribution);
			}
			
		}
			
		return true;
	}*/
	
	
	void put_p_w_u(String w, String u, Double p_w_u){
		HashMap<String, Double> u_distribution = new HashMap<String, Double>();
		if(this.translationmap.containsKey(w)) {
			u_distribution = this.translationmap.get(w);
			this.translationmap.remove(w);
		}
		if(u_distribution.containsKey(u)) {
			//System.err.println("A value has been already recorded for " + w + " | " + u);
			//System.err.println("Not Overriding");
			//this.translationmap.put(w, u_distribution);
			//u_distribution.put(u, p_w_u);
			//do nothing
		}else {
			u_distribution.put(u, p_w_u);
		}
		this.translationmap.put(w, u_distribution);
		
		TreeMultimap<Double, String> u_inverted_distribution = TreeMultimap.create(Ordering.natural().reverse(), Ordering.natural());
		if(this.inverted_translationmap.containsKey(w)) {
			u_inverted_distribution = this.inverted_translationmap.get(w);
			this.inverted_translationmap.remove(w);
		}
		if(u_inverted_distribution.containsEntry(p_w_u, u)) {
			//do nothing
		}else {
			u_inverted_distribution.put(p_w_u, u);
		}
		this.inverted_translationmap.put(w, u_inverted_distribution);
		
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
	
	/** Reads a serialized object of type CooccurenceMap
	 * 
	 * @param filepath - the path of the file on disk containing the CooccurenceMap
	 * @return a CooccurenceMap containing the data on file
	 * @throws IOException 
	 */
	public TranslationMap readmap(String filepath) throws IOException{
		TranslationMap mapInFile = new TranslationMap();
		try{
			FileInputStream fis=new FileInputStream(filepath);
			ObjectInputStream ois=new ObjectInputStream(fis);
			mapInFile=(TranslationMap)ois.readObject();
			ois.close();
			fis.close();
		}catch(Exception e){}
		return mapInFile;
	}
	
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		TranslationMap tm = new TranslationMap();
		tm.put_p_w_u("a", "b", 0.25);
		tm.put_p_w_u("a", "a", 1.0);
		tm.put_p_w_u("a", "c", 0.75);
		
		tm.put_p_w_u("b", "a", 0.65);
		tm.put_p_w_u("b", "b", 1.0);
		tm.put_p_w_u("b", "c", 0.65);
		
		tm.put_p_w_u("c", "c", 1.0);
		
		System.out.println("a|a = " + tm.get_w_u("a", "a"));
		System.out.println("a|b = " + tm.get_w_u("a", "b"));
		System.out.println("a|c = " + tm.get_w_u("a", "c"));
		
		System.out.println("b|a = " + tm.get_w_u("b", "a"));
		System.out.println("b|b = " + tm.get_w_u("b", "b"));
		System.out.println("b|c = " + tm.get_w_u("b", "c"));
		
		System.out.println("c|c = " + tm.get_w_u("c", "c"));
		System.out.println("c|a = " + tm.get_w_u("c", "a")); //this returns 0.0 because an entry has not been set
		
		System.out.println("top 1 a = " + tm.get_topu_givenw("a", 1).toString());
		System.out.println("top 2 a = " + tm.get_topu_givenw("a", 2));
		
		System.out.println("top 1 b = " + tm.get_topu_givenw("b", 1));
		System.out.println("top 2 b = " + tm.get_topu_givenw("b", 2));
		
		System.out.println("top 1 c = " + tm.get_topu_givenw("c", 1));
		System.out.println("top 2 c = " + tm.get_topu_givenw("c", 2)); // thus, returns maximum top t, it could return <t
		
		

	}

}
