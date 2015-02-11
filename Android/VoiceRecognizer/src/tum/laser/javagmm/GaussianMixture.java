package tum.laser.javagmm;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.SoftReference;
import java.util.Random;



/**
 * <b>Gaussian Mixture Model</b>
 *
 * <p>Description</p>
 * This class implements in combination with <code>GaussianComponent</code> a
 * gaussian mixture model. To create a gaussian mixture model you have to
 * specify means, covariances and the weight for each of the gaussian components.
 * <code>GaussianMixture</code> and GaussianComponent do not only model a GMM,
 * but also support training GMMs using the EM algorithm.<br>
 * <br>
 * One noteable aspect regarding the implementation is the fact that if
 * the covariance matrix of any of the components of this GMM gets singular
 * during the training process a <code>CovarianceSingularityException</code> is
 * thrown. The <code>CovarianceSingularityException</code> contains a reduced
 * <code>PointList</code>. All points belonging to the singular component have
 * been removed. So after the reduction one can try to rerun the training
 * algorithem with the reduced <code>PointList</code>.<br>
 * <br>
 * Another aspect of the design of this class was influenced by the limited
 * memory on real world computers. To improve performace a buffer to store some
 * estimations is used. This buffer is <i>static</i> to reduce garbage
 * collection time and all training processes are synchronized on this buffer.
 * Consequently one can only train one GMM instance at a time.<br>
 * <br>
 * <b>New in version 1.1:</b><br>
 * - The cholesky decomposition is used to speed up computations.
 *
 * @see comirva.audio.util.gmm.GaussianComponent
 * @author Klaus Seyerlehner
 * @version 1.1
 */
public final class GaussianMixture implements java.io.Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -3450643365494865923L;
	
	private int dimension = 0;                                                                    //dimension of the gmm
	private GaussianComponent[] components = new GaussianComponent[0];                            //gaussian components

	private static double[][] p_ij = new double[1][1];                                            //hard referece to the buffer of current estimate
	private static SoftReference<double[][]> p_ij_SoftRef = new SoftReference<double[][]>(p_ij);  //soft reference to the buffer of current estimates                                                //defines the maximum number of training iterations
	private static Random rnd = new Random();                                                     //a random number generator


	/**
	 * This constructor creats a GMM and checks the parameters for plausibility.
	 * The weights, means and covarinces of every component are passed as arrays
	 * to the constructor. The i-th component therefore is completely defined by
	 * the i-th entries within these arrays.
	 *
	 * @param componentWeights double[] specifies the components weights
	 * @param means Matrix[] specifies the components mean vectors
	 * @param covariances Matrix[] specifies the components covariance matrices
	 *
	 * @throws IllegalArgumentException if any invalid parameter settings are
	 *                                  detected while checking them
	 */
	public GaussianMixture(double[] componentWeights, Matrix[] means, Matrix[] covariances) throws IllegalArgumentException
	{
		//check if all parameters are valid
		if(componentWeights.length != means.length || means.length != covariances.length || componentWeights.length < 1)
			throw new IllegalArgumentException("all arrays must have the same length with size greater than 0;");

		//create component array
		components = new GaussianComponent[componentWeights.length];

		//check and create the components
		double sum = 0;
		for(int i = 0; i < components.length; i++)
		{
			if(means[i] == null || covariances[i] == null)
				throw new IllegalArgumentException("all mean and covarince matrices must not be null values;");

			sum += componentWeights[i];

			components[i] = new GaussianComponent(componentWeights[i], means[i], covariances[i]);
		}

		//check if the component weights are set correctly
		if( sum < 0.99 || sum > 1.01)
			throw new IllegalArgumentException("the sum over all component weights must be in the interval [0.99, 1.01];");

		//set dimension
		this.dimension = components[0].getDimension();

		//check if all the components have the same dimensions
		for(int i = 0; i < components.length; i++)
			if(components[i].getDimension() != dimension)
				throw new IllegalArgumentException("the dimensions of all components must be the same;");

	}

	/**
	 * This constructor creates a GMM and checks the components for compatibility.
	 * The components themselfs have been checked during their construction.
	 *
	 * @param components GaussianComponent[] an array of gaussian components
	 *
	 * @throws IllegalArgumentException if the passed components are not
	 *                                  compatible
	 */
	public GaussianMixture(GaussianComponent[] components) throws IllegalArgumentException
	{
		if(components == null)
			throw new IllegalArgumentException("the component array must not be null;");

		//check the components
		double sum = 0;
		for(int i = 0; i < components.length; i++)
		{
			if(components[i] == null)
				throw new IllegalArgumentException("all components in the array must not be null;");

			sum += components[i].getComponentWeight();
		}

		//check if the component weights are set correctly
		if( sum < 0.99 || sum > 1.01)
			throw new IllegalArgumentException("the sum over all component weights must be in the interval [0.99, 1.01];");

		this.components = components;
		this.dimension = components[0].getDimension();

		//check if all the components have the same dimensions
		for(int i = 0; i < components.length; i++)
			if(components[i].getDimension() != dimension)
				throw new IllegalArgumentException("the dimensions of all components must be the same;");
	}



	/**
	 * Returns the log likelihood of the points stored in the pointlist under the
	 * assumption the these points where sample from this GMM.<br>
	 * <br>
	 * [SUM over all j: log (SUM over all i:(p(x_j | C = i) * P(C = i)))]
	 *
	 * @param points PointList list of sample points to estimate the log
	 *                              likelihood of
	 * @return double the log likelihood of drawing these samples from this gmm
	 */
	public double getLogLikelihood(PointList points)
	{
		double p = 0;
		for (int j = 0; j < points.size(); j++)
			p += Math.log(getProbability((Matrix) points.get(j)));
		return p;
	}


	/**
	 * Returns the probability of a single sample point under the assumption that
	 * it was draw from the distribution represented by this GMM.<br>
	 * <br>
	 * [SUM over all i:(p(x | C = i) * P(C = i))]
	 *
	 * @param x Matrix a sample point
	 * @return double the probability of the given sample
	 */
	public double getProbability(Matrix x)
	{
		double p = 0;

		for(int i = 0; i < components.length; i++)
			p += components[i].getWeightedSampleProbability(x);

		return p;
	}


	/**
	 * Returns the number of dimensions of the GMM.
	 *
	 * @return int number of dimmensions
	 */
	public int getDimension()
	{
		return dimension;
	}


	/**
	 * Prints some information about this gaussian component.
	 * This is for debugging purpose only.
	 */
	public void print()
	{
		for(int i = 0; i < components.length; i++)
		{
			System.out.println("Component " + i + ":");
			components[i].print();
		}
	}


	/**
	 * For testing purpose only.
	 *
	 * @param numberOfComponent int the number of the component
	 * @return Matrix the mean vector
	 */
	public Matrix getMean(int numberOfComponent)
	{
		return components[numberOfComponent].getMean();
	}


	/**
	 * This method returns a reference to a buffer for storing estimates of the
	 * sample points. The buffer will be reused if possible or reallocated, if
	 * it is too small or if the garbage collector allready captured the buffer.
	 *
	 * @param nrComponents int the number of components of the gmm to allocate the
	 *                         buffer for
	 * @param nrSamplePoints int the number of sample points of the gmm to
	 *                           allocate the buffer for
	 */
	protected static void getBuffer(int nrComponents, int nrSamplePoints)
	{
		//get the buffer from the soft ref => now hard ref
		p_ij = p_ij_SoftRef.get();

		if(p_ij == null)
		{
			//reallocate since gc collected the buffer
			p_ij = new double[nrComponents][2*nrSamplePoints];
			p_ij_SoftRef = new SoftReference<double[][]>(p_ij);
		}

		//check if buffer is too small
		if (p_ij[0].length >= nrSamplePoints && p_ij.length >= nrComponents)
			return;

		//to prevent gc runs take double of the current buffer size
		if (p_ij[0].length < nrSamplePoints)
			nrSamplePoints += nrSamplePoints;

		//reallocate since buffer was too small
		p_ij = new double[nrComponents][nrSamplePoints];
		p_ij_SoftRef = new SoftReference<double[][]>(p_ij);

		//run gc to collect old buffer
		System.gc();
	}
	
	
	/**
	 * Reads a GaussianMixture object by deserializing it from disk
	 * @author Florian Schulze
	 * @param path Path of the serialized GMM file
	 * @return The deserialized GMM
	 */
	public static GaussianMixture readGMM(String path) {
		GaussianMixture gmm = null;
		ObjectInputStream ois;
		try {
			ois = new ObjectInputStream(new FileInputStream(path));
			gmm = (GaussianMixture) ois.readObject();
			ois.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return gmm;
	}
	
	
	/**
	 * Stores a GaussianMixture object on the hard disk by serializing it
	 * @param path Where to store the file
	 */
	public void writeGMM(String path) {
		try {
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path));
			oos.writeObject(this);
			oos.flush();
			oos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
