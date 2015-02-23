package com.example.javagmm;

public class FrequencyProperties {
	private double[] fftReal;
	private double[] fftImag;
	private double[] psdAcrossFrequencyBands;
	private double[] featureCepstrum;
	
	public double[] getFftReal() {
		return fftReal;
	}
	public void setFftReal(double[] fftReal) {
		this.fftReal = fftReal;
	}
	public double[] getFftImag() {
		return fftImag;
	}
	public void setFftImag(double[] fftImag) {
		this.fftImag = fftImag;
	}
	public double[] getPsdAcrossFrequencyBands() {
		return psdAcrossFrequencyBands;
	}
	public void setPsdAcrossFrequencyBands(double[] psdAcrossFrequencyBands) {
		this.psdAcrossFrequencyBands = psdAcrossFrequencyBands;
	}
	public double[] getFeatureCepstrum() {
		return featureCepstrum;
	}
	public void setFeatureCepstrum(double[] featureCepstrum) {
		this.featureCepstrum = featureCepstrum;
	}
}
