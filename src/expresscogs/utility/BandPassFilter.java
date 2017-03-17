package expresscogs.utility;

public class BandPassFilter {
    // FIR filters by Windowing
    // A.Greensted - Feb 2010
    // http://www.labbookpages.co.uk
    public enum filterType {
        LOW_PASS, HIGH_PASS, BAND_PASS, BAND_STOP
    };

    public enum windowType {
        RECTANGULAR, BARTLETT, HANNING, HAMMING, BLACKMAN
    };

    public static void main(String[] args) {
        int windowLength = 201;
        double sampFreq = 44100;

        // Low and high pass filters
        double transFreq = 10000;

        double[] lpf = sincFilter1(windowLength, transFreq, sampFreq, filterType.LOW_PASS);
        double[] lpf_hamming = createWindow(lpf, null, windowLength, windowType.HAMMING);
        double[] lpf_blackman = createWindow(lpf, null, windowLength, windowType.BLACKMAN);

        double[] hpf = sincFilter1(windowLength, transFreq, sampFreq, filterType.HIGH_PASS);
        double[] hpf_hamming = createWindow(hpf, null, windowLength, windowType.HAMMING);

        // outputFFT("lpf-hamming.dat", lpf_hamming, windowLength, sampFreq);
        // outputFFT("lpf-blackman.dat", lpf_blackman, windowLength, sampFreq);
        // outputFFT("hpf-hamming.dat", hpf_hamming, windowLength, sampFreq);

        // Band pass and band stop filters
        double trans1Freq = 5000;
        double trans2Freq = 17050;

        double[] bpf = sincFilter2(windowLength, trans1Freq, trans2Freq, sampFreq, filterType.BAND_PASS);
        double[] bpf_hamming = createWindow(bpf, null, windowLength, windowType.HAMMING);

        double[] bsf = sincFilter2(windowLength, trans1Freq, trans2Freq, sampFreq, filterType.BAND_STOP);
        double[] bsf_hamming = createWindow(bsf, null, windowLength, windowType.HAMMING);

        // outputFFT("bpf-hamming.dat", bpf_hamming, windowLength, sampFreq);
        // outputFFT("bsf-hamming.dat", bsf_hamming, windowLength, sampFreq);
    }

    // Create sinc function for filter with 1 transition - Low and High pass filters
    public static double[] sincFilter1(int windowLength, double transFreq, double sampFreq, filterType type) {
        int n;
        double[] window = new double[windowLength];

        // Calculate the normalised transistion frequency. As transFreq should be
        // less than or equal to sampFreq / 2, ft should be less than 0.5
        double ft = transFreq / sampFreq;

        double m_2 = 0.5 * (windowLength - 1);
        int halfLength = windowLength / 2;

        // Set centre tap, if present
        // This avoids a divide by zero
        if (2 * halfLength != windowLength) {
            double val = 2.0 * ft;

            // If we want a high pass filter, subtract sinc function from a
            // dirac pulse
            if (type == filterType.HIGH_PASS)
                val = 1.0 - val;

            window[halfLength] = val;
        } else if (type == filterType.HIGH_PASS) {
            throw new IllegalArgumentException("create1TransSinc: For high pass filter, window length must be odd\n");
        }

        // This has the effect of inverting all weight values
        if (type == filterType.HIGH_PASS)
            ft = -ft;

        // Calculate taps
        // Due to symmetry, only need to calculate half the window
        for (n = 0; n < halfLength; n++) {
            double val = Math.sin(2.0 * Math.PI * ft * (n - m_2)) / (Math.PI * (n - m_2));

            window[n] = val;
            window[windowLength - n - 1] = val;
        }

        return window;
    }

    // Create two sinc functions for filter with 2 transitions - Band pass and band stop filters
    public static double[] sincFilter2(int windowLength, double transition1Frequency, double transition2Frequency, double sampleFrequency, filterType type) {
        int n;
        double[] window = new double[windowLength];

        // Calculate the normalised transistion frequencies.
        double normalizedFrequency1 = transition1Frequency / sampleFrequency;
        double normalizedFrequency2 = transition2Frequency / sampleFrequency;

        double m_2 = 0.5 * (windowLength - 1);
        int halfLength = windowLength / 2;

        // Set centre tap, if present
        // This avoids a divide by zero
        if (windowLength % 2 == 1) {
            double val = 2.0 * (normalizedFrequency2 - normalizedFrequency1);

            // If we want a band stop filter, subtract sinc functions from a
            // dirac pulse
            if (type == filterType.BAND_STOP)
                val = 1.0 - val;

            window[halfLength] = val;
        } else {
            throw new IllegalArgumentException(
                    "create1TransSinc: For band pass and band stop filters, window length must be odd\n");
        }

        // Swap transition points if Band Stop
        if (type == filterType.BAND_STOP) {
            double tmp = normalizedFrequency1;
            normalizedFrequency1 = normalizedFrequency2;
            normalizedFrequency2 = tmp;
        }

        // Calculate taps
        // Due to symmetry, only need to calculate half the window
        for (n = 0; n < halfLength; n++) {
            double val1 = Math.sin(2.0 * Math.PI * normalizedFrequency1 * (n - m_2)) / (Math.PI * (n - m_2));
            double val2 = Math.sin(2.0 * Math.PI * normalizedFrequency2 * (n - m_2)) / (Math.PI * (n - m_2));

            window[n] = val2 - val1;
            window[windowLength - n - 1] = val2 - val1;
        }

        return window;
    }

    // Create a set of window weights
    // in - If not null, each value will be multiplied with the window weight
    // out - The output weight values, if NULL and new array will be allocated
    // windowLength - the number of weights
    // windowType - The window type
    public static double[] createWindow(double[] in, double[] out, int windowLength, windowType type) {
        // If output buffer has not been allocated, allocate memory now
        if (out == null) {
            out = new double[windowLength];
        }

        int n;
        int m = windowLength - 1;
        int halfLength = windowLength / 2;

        // Calculate taps
        // Due to symmetry, only need to calculate half the window
        switch (type) {
        case RECTANGULAR:
            for (n = 0; n < windowLength; n++) {
                out[n] = 1.0;
            }
            break;

        case BARTLETT:
            for (n = 0; n <= halfLength; n++) {
                double tmp = (double) n - (double) m / 2;
                double val = 1.0 - (2.0 * Math.abs(tmp)) / m;
                out[n] = val;
                out[windowLength - n - 1] = val;
            }

            break;

        case HANNING:
            for (n = 0; n <= halfLength; n++) {
                double val = 0.5 - 0.5 * Math.cos(2.0 * Math.PI * n / m);
                out[n] = val;
                out[windowLength - n - 1] = val;
            }

            break;

        case HAMMING:
            for (n = 0; n <= halfLength; n++) {
                double val = 0.54 - 0.46 * Math.cos(2.0 * Math.PI * n / m);
                out[n] = val;
                out[windowLength - n - 1] = val;
            }
            break;

        case BLACKMAN:
            for (n = 0; n <= halfLength; n++) {
                double val = 0.42 - 0.5 * Math.cos(2.0 * Math.PI * n / m) + 0.08 * Math.cos(4.0 * Math.PI * n / m);
                out[n] = val;
                out[windowLength - n - 1] = val;
            }
            break;
        }

        // If input has been given, multiply with out
        if (in != null) {
            for (n = 0; n < windowLength; n++) {
                out[n] *= in[n];
            }
        }

        return out;
    }

    double modZeroBessel(double x) {
        int i;

        double x_2 = x / 2;
        double num = 1;
        double fact = 1;
        double result = 1;

        for (i = 1; i < 20; i++) {
            num *= x_2 * x_2;
            fact *= i;
            result += num / (fact * fact);
            // printf("%f %f %f\n", num, fact, result);
        }

        return result;
    }

    /*
     * int outputFFT(char *filename, double *window, int windowLength, double
     * sampFreq) { int i; FILE *fp; double *in; fftw_complex *out; fftw_plan
     * plan; int result = 0;
     * 
     * // If the window length is short, zero padding will be used int fftSize =
     * (windowLength < 2048) ? 2048 : windowLength;
     * 
     * // Calculate size of result data int resultSize = (fftSize / 2) + 1;
     * 
     * // Allocate memory to hold input and output data in = (double *)
     * fftw_malloc(fftSize * sizeof(double)); out = (fftw_complex *)
     * fftw_malloc(resultSize * sizeof(fftw_complex)); if (in == NULL || out ==
     * NULL) { result = 1; fprintf(stderr,
     * "outputFFT: Could not allocate input/output data\n"); goto finalise; }
     * 
     * // Create the plan and check for success plan =
     * fftw_plan_dft_r2c_1d(fftSize, in, out, FFTW_MEASURE); if (plan == NULL) {
     * result = 1; fprintf(stderr, "outputFFT: Could not create plan\n"); goto
     * finalise; }
     * 
     * // Copy window and add zero padding (if required) for (i=0 ;
     * i<windowLength ; i++) in[i] = window[i]; for ( ; i<fftSize ; i++) in[i] =
     * 0;
     * 
     * // Perform fft fftw_execute(plan);
     * 
     * // Open file for writing fp = fopen(filename, "w"); if (fp == NULL) {
     * result = 1; fprintf(stderr,
     * "outputFFT: Could open output file for writing\n"); goto finalise; }
     * 
     * // Output result for (i=0 ; i<resultSize ; i++) { double freq = sampFreq
     * * i / fftSize; double mag = sqrt(out[i][0] * out[i][0] + out[i][1] *
     * out[i][1]); double magdB = 20 * log10(mag); double phase =
     * atan2(out[i][1], out[i][0]); fprintf(fp, "%02d %f %f %f %f\n", i, freq,
     * mag, magdB, phase); }
     * 
     * // Perform any cleaning up finalise: if (plan != NULL)
     * fftw_destroy_plan(plan); if (in != NULL) fftw_free(in); if (out != NULL)
     * fftw_free(out); if (fp != NULL) fclose(fp);
     * 
     * return result; }
     */

}
