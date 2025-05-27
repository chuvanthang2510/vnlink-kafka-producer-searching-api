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

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "partial_match_analyzer"),
            otherFields = {
                    @InnerField(suffix = "raw", type = FieldType.Keyword)
            }
    )
    private List<String> serviceType;

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "partial_match_analyzer"),
            otherFields = {
                    @InnerField(suffix = "raw", type = FieldType.Keyword)
            }
    )
    private List<String> serviceCode;

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "partial_match_analyzer"),
            otherFields = {
                    @InnerField(suffix = "raw", type = FieldType.Keyword)
            }
    )
    private List<String> serviceId;

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "partial_match_analyzer"),
            otherFields = {
                    @InnerField(suffix = "raw", type = FieldType.Keyword)
            }
    )
    private String serviceMobile;

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
    private List<String> customerMobile;

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
    private String customerId;

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "partial_match_analyzer"),
            otherFields = {
                    @InnerField(suffix = "raw", type = FieldType.Keyword)
            }
    )
    private String agentId;

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "partial_match_analyzer"),
            otherFields = {
                    @InnerField(suffix = "raw", type = FieldType.Keyword)
            }
    )
    private String saleChannelId;

    @Field(type = FieldType.Date, format = DateFormat.custom, pattern = "dd/MM/yyyy HH:mm:ss")
    private Date orderDate;

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "partial_match_analyzer"),
            otherFields = {
                    @InnerField(suffix = "raw", type = FieldType.Keyword)
            }
    )
    private String orderStatus;

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
    private String customerCareId;

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
    private List<String> subService;
}
