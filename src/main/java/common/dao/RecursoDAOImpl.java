package common.dao;

import common.dominios.*;
import common.dominios.enums.Tabla;
import common.dominios.enums.TipoRecurso;
import common.dominios.enums.TipoTexto;
import common.interfaces.dao.*;

import java.io.Serializable;
import java.sql.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Esta clase implementa la interfaz RecursoDAO y sus métodos para mantener la entidad Recurso
 *
 * @author emilio
 */
public class RecursoDAOImpl implements RecursoDAO {

    private static final String SAVE_RECURSO = "INSERT INTO " + Tabla.Recurso + " (titulo, tipo_recurso, tipo_texto, id_editorial, total_paginas) VALUES (?, ?, ?, ?, ?)";
    private static final String SAVE_LIBRO = "INSERT INTO " + Tabla.Libro + " (isbn, lomo, contraportada, portada, id_recurso) VALUES (?, ?, ?, ?, ?)";
    private static final String SAVE_REVISTA = "INSERT INTO " + Tabla.Revista + " (id_recurso) VALUES (?)";
    private static final String SAVE_PERIODICO = "INSERT INTO " + Tabla.Periodico + " (lema, fecha_publicacion, id_recurso) VALUES (?, ?, ?)";
    private static final String SAVE_RECURSO_AUTOR = "INSERT INTO " + Tabla.Recurso_Autor + " (id_autor, id_recurso) VALUES (? , ?)";
    private static final String SAVE_TOPICO_RECURSO = "INSERT INTO " + Tabla.Topico_Recurso + " (id_topico, id_recurso) VALUES  (?, ?)";
    private static final String FIND_RECURSO_BY_ID = "SELECT * FROM " + Tabla.Recurso + " WHERE id_recurso = ?";
    private static final String FIND_ALL_RECURSO = "SELECT * FROM " + Tabla.Recurso;
    private static final String UPDATE_RECURSO = "UPDATE " + Tabla.Recurso + " SET titulo = ? , tipo_recurso = ?, tipo_texto = ?, id_editorial = ?, total_paginas = ? WHERE id_recurso = ?";
    private static final String UPDATE_LIBRO = "UPDATE " + Tabla.Libro + " SET isbn = ?, lomo = ?, contraportada = ?, portada = ? WHERE id_recurso = ?";
    private static final String UPDATE_PERIODICO = "UPDATE " + Tabla.Periodico + " SET lema = ?, fecha_publicacion = ? WHERE id_recurso = ? ";
    private static final String UPDATE_RECURSO_AUTOR = "UPDATE " + Tabla.Recurso_Autor + " SET id_autor = ? WHERE id_recurso = ?";
    private static final String UPDATE_TOPICO_RECURSO = "UPDATE " + Tabla.Topico_Recurso + " SET id_topico = ? WHERE id_recurso = ?";
    private static final String REMOVE_TOPICO_RECURSO = "DELETE FROM " + Tabla.Topico_Recurso + " WHERE id_recurso = ?";
    private static final String REMOVE_RECURSO_AUTOR = "DELETE FROM " + Tabla.Recurso_Autor + " WHERE id_recurso = ?";
    private static final String REMOVE_RECURSO = "DELETE FROM " + Tabla.Recurso + " WHERE id_recurso = ?";
    private static final String FIND_ALL_BY_TIPO_RECURSO = "SELECT * FROM " + Tabla.Recurso + " WHERE tipo_recurso = ?";
    private static final String FIND_ALL_BY_TIPO_TEXTO = "SELECT * FROM " + Tabla.Recurso + " WHERE tipo_texto = ?";
    private static final String FIND_ALL_BY_TITULO_RECURSO = "SELECT * FROM " + Tabla.Recurso + " WHERE titulo = ?";

    private LibroDAO libroDAO = new LibroDAOImpl();
    private PeriodicoDAO periodicoDAO = new PeriodicoDAOImpl();
    private RevistaDAO revistaDAO = new RevistaDAOImpl();
    private EditorialDAO editorialDAO = new EditorialDAOImpl();
    private TopicoDAO topicoDAO = new TopicoDAOImpl();
    private AutorDAO autorDAO = new AutorDAOImpl();

    private PreparedStatement statement = null;
    private Connection connection = null;
    private ResultSet resultSet = null;

    @Override
    public Recurso find(int id) throws Exception {
        try {
            connection = Database.getConnection();
            statement = connection.prepareStatement(FIND_RECURSO_BY_ID);
            statement.setInt(1, id);
            resultSet = statement.executeQuery();

            Recurso<java.io.Serializable> recurso = getRecursoFromResultset();
            setRecursoComponentsFromDB(recurso);

            return recurso;
        } catch (SQLException e) {
            System.out.println("SQLException in RecursoDAO.find()");
            e.printStackTrace();
            throw new Exception(e.getMessage());
        }
    }

    @Override
    public Set<Recurso> findAll() throws Exception {
        try {
            return findRecursoList("");

        } catch (SQLException e) {
            System.out.println("SQLException in RecursoDAO.findAll()");
            e.printStackTrace();
            throw new Exception(e.getMessage());
        }
    }

    @Override
    public void save(Recurso entity) throws Exception {
        try {
            // Recurso
            connection = Database.getConnection();
            // Auto commit manual para generar un movimiento transaccional y poder hacer rollback en caso de error
            connection.setAutoCommit(false);
            statement = connection.prepareStatement(SAVE_RECURSO, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, entity.getTitulo());
            statement.setInt(2, entity.getTipoRecurso().ordinal());
            statement.setInt(3, entity.getTipoTexto().ordinal());
            statement.setInt(4, entity.getEditorial().getId());
            statement.setInt(5, entity.getTotalPaginas());

            statement.executeUpdate();

            resultSet = statement.getGeneratedKeys();

            // Obtengo clave autogenerada del recurso creado
            int idRecurso = resultSet.next() ? resultSet.getInt(1) : 0;

            if (idRecurso == 0) {
                throw new Exception("No se obtuvo una id de recurso");
            }

            // Recurso_Autor
            persistRecursoAutor(entity, idRecurso, false);

            // Topico_Recurso
            persistTopicoRecurso(entity, idRecurso, false);

            // Libro || Periodico || Revista
            persistRecursoInstance(entity, idRecurso, false);

            // Commit si no hay problemas en los puntos anteriores
            connection.commit();
        } catch (SQLException e) {
            // Rollback en caso de excepcion
            connection.rollback();
            System.out.println("SQLException in RecursoDAO.save()");
            e.printStackTrace();
            throw new Exception(e.getMessage());
        }

    }


    @Override
    public void update(Recurso entity) throws Exception {
        try {
            // Recurso
            connection = Database.getConnection();
            // Auto commit manual para generar un movimiento transaccional y poder hacer rollback en caso de error
            connection.setAutoCommit(false);
            statement = connection.prepareStatement(UPDATE_RECURSO);
            statement.setString(1, entity.getTitulo());
            statement.setInt(2, entity.getTipoRecurso().ordinal());
            statement.setInt(3, entity.getTipoTexto().ordinal());
            statement.setInt(4, entity.getEditorial().getId());
            statement.setInt(5, entity.getTotalPaginas());
            statement.setInt(6, entity.getId());

            int update = statement.executeUpdate();

            if (update == 0) {
                throw new Exception("No realizo ninguna modificacion en entidad Recurso");
            }

            // Autor_Recurso
            persistRecursoAutor(entity, entity.getId(), true);

            // Topico_Recurso
            persistTopicoRecurso(entity, entity.getId(), true);

            Integer id = entity.getId();
            // // Libro || Periodico || Revista
            persistRecursoInstance(entity, entity.getId(), true);

            // Commit si no hay problemas en los puntos anteriores
            connection.commit();
        } catch (SQLException e) {
            // Rollback en caso de excepcion
            connection.rollback();
            System.out.println("SQLException in RecursoDAO.update()");
            e.printStackTrace();
            throw new Exception(e.getMessage());
        }

    }

    @Override
    public void remove(int id) throws Exception {
        try {
            connection = Database.getConnection();
            connection.setAutoCommit(false);
            statement = connection.prepareStatement(REMOVE_RECURSO);
            statement.setInt(1, id);

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                throw new Exception("No eliminó ninguna entidad Recurso");
            }

            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            System.out.println("SQLException in RecursoDAO.remove()");
            e.printStackTrace();
            throw new Exception(e.getMessage());
        }
    }

    @Override
    public Set<Recurso> findByTipoRecurso(TipoRecurso tipoRecurso) throws Exception {
        try {
            return findRecursoListByEnumType(FIND_ALL_BY_TIPO_RECURSO, tipoRecurso.ordinal());

        } catch (SQLException e) {
            System.out.println("SQLException in RecursoDAO.findByTipoRecurso()");
            e.printStackTrace();
            throw new Exception(e.getMessage());
        }
    }

    @Override
    public Set<Recurso> findByTipoTexto(TipoTexto tipoTexto) throws Exception {
        try {
            return findRecursoListByEnumType(FIND_ALL_BY_TIPO_TEXTO, tipoTexto.ordinal());

        } catch (SQLException e) {
            System.out.println("SQLException in RecursoDAO.findByTipoTexto()");
            e.printStackTrace();
            throw new Exception(e.getMessage());
        }
    }

    @Override
    public Set<Recurso> findByName(String name) throws Exception {
        try {
            return findRecursoList(name);

        } catch (SQLException e) {
            System.out.println("SQLException in RecursoDAO.findByName()");
            e.printStackTrace();
            throw new Exception(e.getMessage());
        }
    }

    /**
     * Método helper que realiza la búsqueda en la base de datos por TipoRecurso o TipoTexto
     *
     * @param sql     String de sentencia SQL
     * @param ordinal orden del enum TipoTexto || TipoRecurso
     * @return Set<Recurso> producto de la búsqueda
     * @throws Exception en caso de no encontrar
     */
    private Set<Recurso> findRecursoListByEnumType(String sql, int ordinal) throws Exception {
        connection = Database.getConnection();
        statement = connection.prepareStatement(sql);
        statement.setInt(1, ordinal);

        resultSet = statement.executeQuery();

        Set<Recurso> recursos = new HashSet<>();

        while (resultSet.next()) {
            Recurso recurso = castToRecursoFromResultset();
            setRecursoComponentsFromDB(recurso);
            recursos.add(recurso);
        }

        if (recursos.size() != 0) return recursos;

        throw new Exception("No se encontró ningun recurso");
    }

    /**
     * Método helper que realiza la búsqueda en la DB de: (1) todos los recursos si recibe un String vacío o
     * (2) todos los recursos que coincidan con el mismo tituloRecurso proporcionado
     *
     * @param tituloRecurso String titulo del recurso
     * @return Set<Recurso> pruducto de la búsqueda
     * @throws Exception en caso de no encontrar
     */
    private Set<Recurso> findRecursoList(String tituloRecurso) throws Exception {
        connection = Database.getConnection();
        statement = connection.prepareStatement(tituloRecurso.equals("") ? FIND_ALL_RECURSO : FIND_ALL_BY_TITULO_RECURSO);
        if (!tituloRecurso.equals("")) statement.setString(1, tituloRecurso);


        resultSet = statement.executeQuery();

        Set<Recurso> recursos = new HashSet<>();

        while (resultSet.next()) {
            Recurso recurso = castToRecursoFromResultset();
            setRecursoComponentsFromDB(recurso);
            recursos.add(recurso);
        }

        if (recursos.size() != 0) return recursos;

        throw new Exception("No se encontró ningun recurso");
    }

    /**
     * Realiza la búsqueda de todos los componentes de la entidad Recurso proporcionada, complementado
     * sus respectivas id's
     *
     * @param recurso Entidad
     * @throws Exception en caso de no encontrar algún componente.
     */
    private void setRecursoComponentsFromDB(Recurso<Serializable> recurso) throws Exception {
        if (recurso == null) throw new Exception("No se encontró ningun recurso con ese id");

        int idRecurso = recurso.getId();
        int idEditorial = recurso.getEditorial().getId();

        switch (recurso.getTipoRecurso()) {
            case LIBRO:
                recurso.setRecurso(libroDAO.find(idRecurso));
                break;

            case PERIODICO:
                recurso.setRecurso(periodicoDAO.find(idRecurso));
                break;

            case REVISTA:
                recurso.setRecurso(revistaDAO.find(idRecurso));
                break;
        }

        recurso.setEditorial(editorialDAO.find(idEditorial));
        recurso.setTopicos(topicoDAO.findListById(idRecurso));
        recurso.setAutores(autorDAO.findListById(idRecurso));
    }

    /**
     * Método helper que realiza la persistencia en DB de la entidad Topico_Recurso. Si update es (1) true
     * realiza la modificacion de los datos y si es (2) false realiza la simple persistencia.
     *
     * @param entity    Recurso para obtener el autor asociado
     * @param idRecurso int id del recurso
     * @param update    boolean
     * @throws Exception en caso de encontrar algun error
     */
    private void persistTopicoRecurso(Recurso entity, int idRecurso, boolean update) throws Exception {
        String exceptionModifier = update ? "modificó" : "insertó";

        statement = connection.prepareStatement(update ? UPDATE_TOPICO_RECURSO : SAVE_TOPICO_RECURSO);
//        Set<Topico> topicos = entity.getTopicos();

        for (Topico topico : (Set<Topico>) entity.getTopicos()) {
            statement.setInt(1, topico.getId());
            statement.setInt(2, idRecurso);
            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new Exception("No se " + exceptionModifier + " un topico");
            }
        }
    }

    /**
     * Método helper que realiza la persistencia en DB de la entidad Recurso_Autor. Si update es (1) true
     * realiza la modificacion de los datos y si es (2) false realiza la simple persistencia.
     *
     * @param entity    Recurso
     * @param idRecurso int
     * @param update    boolean
     * @throws Exception en caso de error
     */
    private void persistRecursoAutor(Recurso entity, int idRecurso, boolean update) throws Exception {
        String exceptionModifier = update ? "modificó" : "insertó";

        statement = connection.prepareStatement(update ? UPDATE_RECURSO_AUTOR : SAVE_RECURSO_AUTOR);
        Set<Autor> autores = entity.getAutores();
        for (Autor autor : autores) {
            statement.setInt(1, autor.getId());
            statement.setInt(2, idRecurso);
            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new Exception("No se " + exceptionModifier + " un autor del libro");
            }
        }
    }

    /**
     * Método helper que realiza la persistencia en DB de la entidad Libro || Revista || Periodico. Si update
     * es (1) true realiza la modificacion de los datos y si es (2) false realiza la simple persistencia.
     *
     * @param entity    Recurso
     * @param idRecurso int
     * @param update    boolean
     * @throws Exception en caso de error
     */
    private void persistRecursoInstance(Recurso entity, int idRecurso, boolean update) throws Exception {
        String exceptionModifier = update ? "modificó" : "insertó";

        // Libro
        if (entity.getRecurso() instanceof Libro) {
            Libro libro = (Libro) entity.getRecurso();

            statement = connection.prepareStatement(update ? UPDATE_LIBRO : SAVE_LIBRO);
            statement.setString(1, libro.getIsbn());
            statement.setString(2, libro.getLomo());
            statement.setString(3, libro.getContraportada());
            statement.setString(4, libro.getPortada());
            statement.setInt(5, idRecurso);

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                throw new Exception("No se " + exceptionModifier + " ningun Libro");
            }
        }

        // Periodico
        if (entity.getRecurso() instanceof Periodico) {
            Periodico periodico = (Periodico) entity.getRecurso();

            statement = connection.prepareStatement(update ? UPDATE_PERIODICO : SAVE_PERIODICO);
            statement.setString(1, periodico.getLema());
            statement.setDate(2, periodico.getFechaPublicacion());
            statement.setInt(3, idRecurso);
            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                throw new Exception("No se " + exceptionModifier + " ningun periodico");
            }
        }

        // Revista
        if (entity.getRecurso() instanceof Revista) {
            if (!update) {
                Revista revista = (Revista) entity.getRecurso();

                statement = connection.prepareStatement(SAVE_REVISTA);
                statement.setInt(1, idRecurso);
                int affectedRows = statement.executeUpdate();

                if (affectedRows == 0) {
                    throw new Exception("No se " + exceptionModifier + " ningun periodico");
                }
            }
        }
    }

    /**
     * Método helper que obtiene un Recurso desde el ResultSet
     *
     * @return Recurso
     * @throws SQLException en caso de problemas con la DB
     */
    private Recurso<java.io.Serializable> getRecursoFromResultset() throws SQLException {
        if (resultSet.next()) {
            return castToRecursoFromResultset();
        }
        return null;
    }

    /**
     * Método helper que realiza el casteo desde un ResultSet a un Recurso
     *
     * @return Recurso
     * @throws SQLException en caso de error en la DB
     */
    private Recurso<java.io.Serializable> castToRecursoFromResultset() throws SQLException {
        Recurso<java.io.Serializable> recurso = new Recurso<>();
        recurso.setTotalPaginas(resultSet.getInt("total_paginas"));
        recurso.setId(resultSet.getInt("id_recurso"));
        recurso.setTitulo(resultSet.getString("titulo"));
        recurso.setTipoTexto(TipoTexto.values()[resultSet.getInt("tipo_texto")]);
        recurso.setTipoRecurso(TipoRecurso.values()[resultSet.getInt("tipo_recurso")]);
        Editorial editorial = new Editorial();
        recurso.setEditorial(editorial);
        recurso.getEditorial().setId(resultSet.getInt("id_editorial"));
        return recurso;
    }
}
