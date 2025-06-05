package vnlink.com.vn.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;


@Data
public class ServiceInfo {
    @Id
    private String id;

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "partial_match_analyzer"),
            otherFields = @InnerField(suffix = "raw", type = FieldType.Keyword)
    )
    private String serviceId;

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "partial_match_analyzer"),
            otherFields = @InnerField(suffix = "raw", type = FieldType.Keyword)
    )
    private String serviceCode;

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "partial_match_analyzer"),
            otherFields = @InnerField(suffix = "raw", type = FieldType.Keyword)
    )
    private String code;

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "partial_match_analyzer"),
            otherFields = @InnerField(suffix = "raw", type = FieldType.Keyword)
    )
    private String subService;
}

