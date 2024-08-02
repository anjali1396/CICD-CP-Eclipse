
package v2.managers;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import javax.activation.MimetypesFileTypeMap;

import org.apache.commons.codec.binary.Base64;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.ObjectTagging;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.Tag;

import utils.Constants;
import utils.LoggerUtils;
import utils.ProptertyUtils;
import utils.Constants.CredType;
import utils.ProptertyUtils.Keys;
import v1.repository.CommonRepository;
import models.Creds;

public class AmazonClient {

	private final AmazonS3 s3client;
	private final Regions clientRegion;
	private final String awsId;
	private final String awsKey;
	private final String BUCKET_NAME_PROD = "homefirstindia-s3bucket";
	private final String BUCKET_NAME_TEST = "hffc-teststaging-s3";
	private static Creds _creds = null;

	
	private static void log(String value) {
		LoggerUtils.log("AmazonClient." + value);
	}
	
	public AmazonClient() throws Exception {
		
		var amazonCreds = amazonCreds();
		
		clientRegion = Regions.AP_SOUTH_1;
		
		awsId = ProptertyUtils.getKeyBearer().decrypt(amazonCreds.username);
		awsKey = ProptertyUtils.getKeyBearer().decrypt(amazonCreds.password);
		
		
		BasicAWSCredentials awsCreds = new BasicAWSCredentials(awsId,
				awsKey);
		s3client = AmazonS3ClientBuilder.standard().withRegion(clientRegion)
				.withCredentials(new AWSStaticCredentialsProvider(awsCreds)).build();
	}
	
	private static Creds amazonCreds() throws Exception {
		
		if (null == _creds) {
			_creds = new CommonRepository().findCredsByPartnerName(Constants.PARTNER_AMAZON, CredType.PRODUCTION);

			if (null == _creds) {
				log("amazonCreds - failed to get Amazon Creds from DB.");
				throw new Exception("failed to get Amazon Creds from DB.");
			}

		}
		return _creds;
		
	}
	

	public enum S3BucketPath {
		PROFILE_IMAGES("HFCustomerPortal/Profile_picture"),
		RESOURCE_PROMOTION("external/promotion/"),
		RESOURCE_NOTIFICATION("external/notification"),
		NOTIFICATION("HFCustomerPortal/Notification"),
		SERVICE_REQUEST("HFCustomerPortal/ServiceRequest");

		public final String stringValue;

		S3BucketPath(String stringValue) {
			this.stringValue = stringValue;
		}
		

	}
	
	
	private String getBucketName() {
	
		if (Constants.IS_PRODUCTION)
			return BUCKET_NAME_PROD;
		else return BUCKET_NAME_TEST;
		
	}

	
	public boolean uploadImage(
			String fileName, 
			String fileData, 
			S3BucketPath bucketPath
	) throws Exception {
		
		try {

			MimetypesFileTypeMap mimetypesFileTypeMap = new MimetypesFileTypeMap();
			byte[] bytes = Base64.decodeBase64(fileData.getBytes());
			ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);

			ObjectMetadata metadata = new ObjectMetadata();
			metadata.setContentType(mimetypesFileTypeMap.getContentType(fileName));
			metadata.setContentLength(bytes.length);
			metadata.addUserMetadata("x-amz-meta-title", fileName);

			List<Tag> tags = new ArrayList<Tag>();
			tags.add(new Tag("Classification", "profile_picture"));

			PutObjectRequest request = new PutObjectRequest(getBucketName(), bucketPath.stringValue + "/" + fileName,
					byteArrayInputStream, metadata);
			request.setTagging(new ObjectTagging(tags));
			s3client.putObject(request);
			LoggerUtils.log("==> File saved successfully in S3 with Name: " + fileName);

			return true;
		
		} catch (AmazonServiceException e) {
			// The call was transmitted successfully, but Amazon S3 couldn't process
			// it, so it returned an error response.
			e.printStackTrace();
		} catch (SdkClientException e) {
			// Amazon S3 couldn't be contacted for a response, or the client
			// couldn't parse the response from Amazon S3.
			e.printStackTrace();
		}
		
		return false;
		
	}
	
	public String getFullUrl(String fileName, S3BucketPath bucketPath) {

		if (null != fileName && !fileName.equalsIgnoreCase(Constants.NA))
			return s3client.getUrl(getBucketName(), bucketPath.stringValue + "/" + fileName).toString();
		else
			return Constants.NA;

	}
	
	public String getBaseUrl(S3BucketPath bucketPath) {
		return s3client.getUrl(getBucketName(), bucketPath.stringValue).toString();
	}

}
