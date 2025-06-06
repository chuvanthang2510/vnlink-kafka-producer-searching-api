package vnlink.com.vn.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.util.Date;
import java.util.List;

@Data
@Document(indexName = "orders*")
@Setting(
        settingPath = "vietnamese-analyzer.json",
        shards = 5,
        replicas = 1,
        refreshInterval = "30s"
)
public class OrderDocument {
    @Id
    private String id;

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "partial_match_analyzer"),
            otherFields = {
                    @InnerField(suffix = "raw", type = FieldType.Keyword)
            }
    )
    private String orderCode;

    @Field(type = FieldType.Date, format = DateFormat.custom, pattern = "dd/MM/yyyy HH:mm:ss")
    private Date createdDate;

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "partial_match_analyzer"),
            otherFields = {
                    @InnerField(suffix = "raw", type = FieldType.Keyword)
            }
    )
    private String saleChannelId;

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "partial_match_analyzer"),
            otherFields = {
                    @InnerField(suffix = "raw", type = FieldType.Keyword)
            }
    )
    private String agentId;

    @MultiField(
            mainField = @Field(type = FieldType.Text,
                    analyzer = "vietnamese_analyzer",
                    searchAnalyzer = "vietnamese_search_analyzer"),
            otherFields = {
                    @InnerField(suffix = "raw", type = FieldType.Keyword)
            }
    )
    private String customerName;

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "partial_match_analyzer"),
            otherFields = {
                    @InnerField(suffix = "raw", type = FieldType.Keyword)
            }
    )
    private String customerMobile;

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "partial_match_analyzer"),
            otherFields = {
                    @InnerField(suffix = "raw", type = FieldType.Keyword)
            }
    )
    private String customerEmail;

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "partial_match_analyzer"),
            otherFields = {
                    @InnerField(suffix = "raw", type = FieldType.Keyword)
            }
    )
    private String emailPrefix;

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "partial_match_analyzer"),
            otherFields = {
                    @InnerField(suffix = "raw", type = FieldType.Keyword)
            }
    )
    private String customerId;

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "partial_match_analyzer"),
            otherFields = {
                    @InnerField(suffix = "raw", type = FieldType.Keyword)
            }
    )
    private String paymentStatus;

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "partial_match_analyzer"),
            otherFields = {
                    @InnerField(suffix = "raw", type = FieldType.Keyword)
            }
    )
    private String agentPaymentStatus;

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "partial_match_analyzer"),
            otherFields = {
                    @InnerField(suffix = "raw", type = FieldType.Keyword)
            }
    )
    private String customerCareId;

    @Field(type = FieldType.Nested, includeInParent = true)
    private List<ServiceInfo> services;
}
