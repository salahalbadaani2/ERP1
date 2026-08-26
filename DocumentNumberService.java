import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** مولد أرقام مستقل ومتسلسل لكل نوع مستند، مع الحفاظ على الأرقام القديمة. */
public final class DocumentNumberService {
    private DocumentNumberService() {
    }

    public static synchronized String next(String documentType, String prefix) {
        if (documentType == null || documentType.trim().isEmpty()) {
            throw new IllegalArgumentException("نوع المستند مطلوب.");
        }
        if (prefix == null || prefix.trim().isEmpty()) {
            throw new IllegalArgumentException("بادئة المستند مطلوبة.");
        }
        String create = "CREATE TABLE IF NOT EXISTS document_sequences ("
                + "document_type VARCHAR(50) PRIMARY KEY, next_number INT NOT NULL) ENGINE=InnoDB";
        String select = "SELECT next_number FROM document_sequences WHERE document_type = ? FOR UPDATE";
        String insert = "INSERT INTO document_sequences (document_type, next_number) VALUES (?, 2)";
        String update = "UPDATE document_sequences SET next_number = next_number + 1 WHERE document_type = ?";

        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(create)) {
                statement.executeUpdate();
            }
            int number;
            try (PreparedStatement statement = connection.prepareStatement(select)) {
                statement.setString(1, documentType.trim());
                try (ResultSet result = statement.executeQuery()) {
                    if (result.next()) {
                        number = result.getInt(1);
                        try (PreparedStatement updateStatement = connection.prepareStatement(update)) {
                            updateStatement.setString(1, documentType.trim());
                            updateStatement.executeUpdate();
                        }
                    } else {
                        number = 1;
                        try (PreparedStatement insertStatement = connection.prepareStatement(insert)) {
                            insertStatement.setString(1, documentType.trim());
                            insertStatement.executeUpdate();
                        }
                    }
                }
            }
            connection.commit();
            return prefix + number;
        } catch (SQLException exception) {
            throw new IllegalStateException("تعذر توليد رقم " + documentType + ": " + exception.getMessage(), exception);
        }
    }
}
