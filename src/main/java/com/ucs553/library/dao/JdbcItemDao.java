package com.ucs553.library.dao;

import com.ucs553.library.model.Book;
import com.ucs553.library.model.DigitalResource;
import com.ucs553.library.model.Item;
import com.ucs553.library.model.ItemType;
import com.ucs553.library.model.Magazine;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class JdbcItemDao extends AbstractJdbcDao<Item> implements ItemDao {

    public JdbcItemDao(ConnectionSource source) {
        super(source, "items");
    }

    @Override
    public List<Item> search(String text) throws DataAccessException {
        String like = "%" + text.trim().toLowerCase() + "%";
        return query("SELECT * FROM items WHERE LOWER(title) LIKE ? OR LOWER(author) LIKE ? ORDER BY title",
                ps -> {
                    ps.setString(1, like);
                    ps.setString(2, like);
                }, this::mapRow);
    }

    @Override
    protected Item mapRow(ResultSet rs) throws SQLException {
        ItemType type = ItemType.valueOf(rs.getString("type"));
        String title = rs.getString("title");
        String author = rs.getString("author");
        int year = rs.getInt("year");
        int copies = rs.getInt("total_copies");

        Item item = switch (type) {
            case BOOK -> new Book.Builder().title(title).author(author).year(year).copies(copies)
                    .isbn(rs.getString("isbn")).pages(rs.getInt("pages")).build();
            case MAGAZINE -> new Magazine(title, author, year, copies,
                    rs.getInt("issue_number"), rs.getString("publisher"));
            case DIGITAL -> new DigitalResource(title, author, year, copies,
                    rs.getString("format"), rs.getDouble("size_mb"));
        };
        item.setId(rs.getInt("id"));
        item.restoreAvailableCopies(rs.getInt("available_copies"));
        return item;
    }

    @Override
    protected String insertSql() {
        return "INSERT INTO items (type, title, author, year, total_copies, available_copies, "
                + "isbn, pages, issue_number, publisher, format, size_mb) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
    }

    @Override
    protected String updateSql() {
        return "UPDATE items SET type=?, title=?, author=?, year=?, total_copies=?, available_copies=?, "
                + "isbn=?, pages=?, issue_number=?, publisher=?, format=?, size_mb=? WHERE id=?";
    }

    @Override
    protected void bindInsert(PreparedStatement ps, Item item) throws SQLException {
        bindColumns(ps, item);
    }

    @Override
    protected void bindUpdate(PreparedStatement ps, Item item) throws SQLException {
        bindColumns(ps, item);
        ps.setInt(13, item.getId());
    }

    private void bindColumns(PreparedStatement ps, Item item) throws SQLException {
        ps.setString(1, item.getType().name());
        ps.setString(2, item.getTitle());
        ps.setString(3, item.getAuthor());
        ps.setInt(4, item.getYear());
        ps.setInt(5, item.getTotalCopies());
        ps.setInt(6, item.getAvailableCopies());

        String isbn = "";
        int pages = 0;
        int issue = 0;
        String publisher = "";
        String format = "";
        double sizeMb = 0;
        if (item instanceof Book b) {
            isbn = b.getIsbn();
            pages = b.getPages();
        } else if (item instanceof Magazine m) {
            issue = m.getIssueNumber();
            publisher = m.getPublisher();
        } else if (item instanceof DigitalResource d) {
            format = d.getFormat();
            sizeMb = d.getSizeMb();
        }
        ps.setString(7, isbn);
        ps.setInt(8, pages);
        ps.setInt(9, issue);
        ps.setString(10, publisher);
        ps.setString(11, format);
        ps.setDouble(12, sizeMb);
    }

    @Override
    protected int idOf(Item item) {
        return item.getId();
    }

    @Override
    protected void assignId(Item item, int id) {
        item.setId(id);
    }
}
