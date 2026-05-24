package dk.easv.scanova.BLL;

import dk.easv.scanova.BE.*;
import dk.easv.scanova.DAL.DocumentDAO;
import dk.easv.scanova.DAL.FileDAO;

public class ScanSessionService {

    private final DocumentDAO documentDAO;
    private final FileDAO fileDAO;

    public ScanSessionService(DocumentDAO documentDAO, FileDAO fileDAO) {
        this.documentDAO = documentDAO;
        this.fileDAO = fileDAO;
    }

    public void handleIncomingFile(ScanSession session, ScannedFile file) {

        validateSession(session);

        file.setFileOrderId(session.nextFileOrderId());

        Document currentDocument = session.getCurrentDocument();

        if (currentDocument == null) {

            currentDocument = documentDAO.createDocument(
                    session.getBox().getId()
            );

            currentDocument.setStatus(Document.DocumentStatus.IN_PROGRESS);

            session.setCurrentDocument(currentDocument);

            System.out.println("📄 First document created: " +
                    currentDocument.getDocumentId());
        } else if (file.isBarcodeDetected()) {

            currentDocument.setStatus(
                    Document.DocumentStatus.WAITING_FOR_QA
            );

            currentDocument = documentDAO.createDocument(
                    session.getBox().getId()
            );

            currentDocument.setStatus(
                    Document.DocumentStatus.IN_PROGRESS
            );

            session.setCurrentDocument(currentDocument);

            System.out.println("📄 New document created after barcode: " +
                    currentDocument.getDocumentId());
        }

        file.setDocumentId(currentDocument.getDocumentId());

        persistFile(file);

        currentDocument.addFile(file);
    }

    private void persistFile(ScannedFile file) {

        System.out.println("Saving file to DB: " + file.getFilePath());

        try {
            fileDAO.insertFile(
                    file.getDocumentId(),
                    file.getReferenceId(),
                    file.getFileOrderId(),
                    file.getFilePath(),
                    file.getRotation(),
                    file.isBarcodeDetected(),
                    SessionManager.getInstance().getCurrentUser().getId()
            );

        } catch (Exception e) {
            throw new RuntimeException("Could not persist file", e);
        }
    }

    private void validateSession(ScanSession session) {

        if (session == null) {
            throw new IllegalStateException("Session is null");
        }

        if (session.getBox() == null) {
            throw new IllegalStateException("Box missing");
        }
    }
}


