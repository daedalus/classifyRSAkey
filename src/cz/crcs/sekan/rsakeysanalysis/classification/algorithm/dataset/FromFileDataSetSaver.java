package cz.crcs.sekan.rsakeysanalysis.classification.algorithm.dataset;

import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.BatchHolder;
import cz.crcs.sekan.rsakeysanalysis.classification.key.ClassificationKey;
import cz.crcs.sekan.rsakeysanalysis.classification.key.ClassificationKeyStub;
import cz.crcs.sekan.rsakeysanalysis.classification.table.ClassificationContainer;
import cz.crcs.sekan.rsakeysanalysis.common.ExtendedWriter;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigInteger;
import java.util.Map;
import java.util.TreeMap;

/**
 * Outputs dataset with added information about the classification.
 * Requires access to the original file for reconstructing the dataset.
 *
 * @author xnemec1
 * @version 11/24/16.
 */
public class FromFileDataSetSaver implements DataSetSaver {
    private DataSetIterator dataSetIterator;

    private Map<Long, ClassificationContainer> batchIdToContainer;
    private Map<BigInteger, Long> keyModulusToKeyId;
    private DataSetFormatter dataSetFormatter;
    private ExtendedWriter resultWriter;

    public FromFileDataSetSaver(DataSetIterator dataSetIterator, DataSetFormatter dataSetFormatter, ExtendedWriter resultWriter) {
        this.dataSetIterator = dataSetIterator;
        this.dataSetFormatter = dataSetFormatter;
        this.resultWriter = resultWriter;
        batchIdToContainer = new TreeMap<>();
        keyModulusToKeyId = new TreeMap<>();
    }

    @Override
    public void registerKeyUnderKeyId(ClassificationKey key, Long keyId) {
        keyModulusToKeyId.put(shortenModulus(key.getRsaKey().getModulus()), keyId);
    }

    @Override
    public void setBatchClassificationResult(Long batchId, ClassificationContainer classificationContainer) {
        batchIdToContainer.put(batchId, classificationContainer);
    }

    @Override
    public void reconstructDataSet(BatchHolder batchHolder, Map<Long, ClassificationKeyStub> keyIdToKeyStub) {
        while (dataSetIterator.hasNext()) {
            ClassificationKey key = dataSetIterator.next();
            BigInteger modulus = key.getRsaKey().getModulus();
            Long keyId = keyModulusToKeyId.get(shortenModulus(modulus));
            Long batchId = batchHolder.getBatchIdForKeyId(keyId);
            if (batchId == null) continue; // key was not parsed previously, e.g. mask could not be extracted
            ClassificationContainer container = batchIdToContainer.get(batchId);
            ClassificationKeyStub stub = keyIdToKeyStub.get(keyId);
            if (stub != null) key.setIdentification(stub.getMask());
            try {
                resultWriter.writeln(dataSetFormatter.classifiedKeyToLine(key, container));
            } catch (IOException e) {
                System.err.println("Could not save result of classification: " + e.getMessage());
            }
        }
        dataSetIterator.close();
    }

    private static final BigInteger mod = BigInteger.ZERO.setBit(128);

    // shorten the modulus before saving it to conserve memory; this could use a hash function, but low 128 bits differ
    private static BigInteger shortenModulus(BigInteger modulus) {
        return modulus.mod(mod);
    }
}
