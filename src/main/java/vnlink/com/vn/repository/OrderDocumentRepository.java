package vnlink.com.vn.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import vnlink.com.vn.model.OrderDocument;

@Repository
public interface OrderDocumentRepository extends ElasticsearchRepository<OrderDocument, String> {
}