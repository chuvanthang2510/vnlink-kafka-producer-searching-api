package vnlink.com.vn.repository;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import vnlink.com.vn.model.Order;

@Repository
public interface OrderRepository extends ElasticsearchRepository<Order, String> {
} 