package dk.easv.scanova.BLL;

import dk.easv.scanova.Model.Document;
import dk.easv.scanova.Model.ScannedFile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScanManagerTest {

    // Test that a new ScannedFile starts with rotation 0
    @Test
    void newFile_shouldHaveZeroRotation() {
        ScannedFile file = new ScannedFile(1, 1, new byte[]{1, 2, 3}, 1);
        assertEquals(0, file.getRotation());
    }

    // Test that we can change the rotation on a file
    @Test
    void setRotation_shouldUpdateCorrectly() {
        ScannedFile file = new ScannedFile(1, 1, new byte[]{1, 2, 3}, 1);
        file.setRotation(90);
        assertEquals(90, file.getRotation());
    }

    // Test that rotating right 4 times brings us back to 0
    @Test
    void rotateRight4Times_shouldReturnToZero() {
        int rotation = 0;
        for (int i = 0; i < 4; i++) {
            rotation = (rotation + 90) % 360;
        }
        assertEquals(0, rotation);
    }

    // Test that rotating left from 0 gives 270
    @Test
    void rotateLeftFromZero_shouldGive270() {
        int rotation = 0;
        rotation = (rotation - 90 + 360) % 360;
        assertEquals(270, rotation);
    }

    // Test that a new document starts with 0 files
    @Test
    void newDocument_shouldHaveNoFiles() {
        Document doc = new Document(1);
        assertEquals(0, doc.getFiles().size());
    }

    // Test that we can add a file to a document
    @Test
    void addFile_shouldIncreaseFileCount() {
        Document doc = new Document(1);
        ScannedFile file = new ScannedFile(1, 1, new byte[]{1}, 1);
        doc.addFile(file);
        assertEquals(1, doc.getFiles().size());
    }
}
