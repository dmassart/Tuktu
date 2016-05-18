package tuktu.api

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.URI

import scala.io.Codec
import scala.io.Source

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.Path

import com.amazonaws.auth.AWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.GetObjectRequest
import com.netaporter.uri.Uri
import java.net.URLDecoder

class S3CredentialProvider(id: String, key: String) extends AWSCredentials {
    override def getAWSAccessKeyId() = id
    override def getAWSSecretKey() = key
}

object file {
    /**
     * A Generic Reader for multiple sources
     */
    def genericReader(uri: URI)(implicit codec: Codec): BufferedReader = {
        uri.getScheme match {
            case "file" | "" | null => fileReader(uri)
            case "hdfs"             => hdfsReader(uri)
            case _                  => throw new Exception("Unknown file format")
        }
    }

    /**
     * Wrapper for a string instead of URI
     */
    def genericReader(string: String)(implicit codec: Codec): BufferedReader = {
        // Get URI
        val uri = Uri.parse(string).toURI
        if (uri.getScheme == "s3") s3Reader(string)(codec)
        else genericReader(Uri.parse(string).toURI)(codec)
    }

    /**
     * Reads from Local disk
     */
    def fileReader(uri: URI)(implicit codec: Codec): BufferedReader = {
        if (uri.toString.startsWith("//"))
            Source.fromFile(uri.getHost + File.separator + uri.getPath)(codec).bufferedReader
        else
            Source.fromFile(uri.getPath)(codec).bufferedReader
    }

    /**
     * Reads from HDFS
     */
    def hdfsReader(uri: URI)(implicit codec: Codec): BufferedReader = {
        val conf = new Configuration
        conf.set("fs.defaultFS", uri.getHost + ":" + uri.getPort)
        val fs = FileSystem.get(conf)
        val path = new Path(uri.getPath)
        new BufferedReader(new InputStreamReader(fs.open(path), codec.decoder))
    }
    
    /**
     * Reads from S3
     */
    def s3Reader(address: String)(implicit codec: Codec): BufferedReader = {
        val split = address.drop(5).split("@")
        
        // Get credentials from address, must be URL encoded and before @
        val (id, key) = {
            val userInfo = split.head.split(":")
            (userInfo(0), URLDecoder.decode(userInfo(1), codec.name))
        }
        
        // Set up S3 client
        val s3Client = new AmazonS3Client(new S3CredentialProvider(id, key))
        
        // Get the actual object
        val parts = split.drop(1).mkString("@").split("/")
        val bucketName = parts.head
        val keyName = parts.drop(1).mkString("/")
        val s3Object = s3Client.getObject(new GetObjectRequest(bucketName, keyName))

        // Return buffered reader
        new BufferedReader(new InputStreamReader(s3Object.getObjectContent, codec.decoder))
    }
}