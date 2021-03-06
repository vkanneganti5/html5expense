package com.springsource.html5expense.services.utilities;

import com.mongodb.DB;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSFile;
import com.mongodb.gridfs.GridFSInputFile;
import org.apache.commons.io.IOUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.DbCallback;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.inject.Inject;
import java.io.InputStream;

/**
 * Simple utility class to interface with MongoDB's GridFS
 * abstraction, which can be quite handy for storing files.
 *
 * NB: it's expected that more mature support will eventually be available in the Spring Data Mongo
 * project, so this is expected to be phased out, at some point.
 *
 * @author Josh Long
 */
@Component
public class MongoDbGridFsUtilities {

    private MongoTemplate mongoTemplate;

    @Inject
    public MongoDbGridFsUtilities(MongoTemplate mongoTemplate){
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * A simple utility to write objects to the database
     *
     * @param bucket        the name of the collection to store it into
     * @param content       the content to write
     * @param filename      the file name to use for the file (in practice, this could be any String)
     * @param metadata      the metadata to associate with this information (it's optional)
     * @return a {@link GridFSFile} that represents the written data.
     */
    public GridFSFile write( final String bucket, final InputStream content, final String filename, final DBObject metadata) {
        Assert.notNull(content);
        Assert.hasText(filename);
        return mongoTemplate.execute(new DbCallback<GridFSInputFile>() {
            @Override
            public GridFSInputFile doInDB(DB db) throws MongoException, DataAccessException {
                GridFSInputFile file = gridFs(db, bucket).createFile(content, filename, true);
                file.setFilename(filename);
                if (null != metadata)
                    file.setMetaData(metadata);
                file.save();
                IOUtils.closeQuietly(content);
                return file;
            }
        });
    }

    /**
     * Reads file data from MongoDB
     *
     * @param bucket        the name of the collection to put the file
     * @param fileName      the name of the file
     * @return an InputStream that the application can read from.
     */
    public InputStream read( final String bucket, final String fileName) {
        return mongoTemplate.executeInSession(new DbCallback<InputStream>() {
            @Override
            public InputStream doInDB(DB db) throws MongoException, DataAccessException {
                GridFS gridFS = gridFs(db, bucket);
                GridFSDBFile file = gridFS.findOne(fileName);
                return file.getInputStream();
            }
        });
    }

    private static GridFS gridFs(DB db, String bucket) {
        return new GridFS(db, bucket);
    }
}
