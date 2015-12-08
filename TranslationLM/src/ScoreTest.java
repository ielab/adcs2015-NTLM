import org.terrier.matching.models.WeightingModelLibrary;



public class ScoreTest {

	public static void main(String[] args) {
		double tf = 1;
		double c = 2500;
		double numberOfTokens = 2;
		double colltermFrequency = 1;
		double docLength = 1;
		double score =  WeightingModelLibrary.log(1 + (tf/(c * (colltermFrequency / numberOfTokens))) ) + WeightingModelLibrary.log(c/(docLength+c));

		System.out.println(score);
	}

}
