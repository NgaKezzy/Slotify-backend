package com.slotify.module.salon.repository;

import com.slotify.module.salon.entity.Category;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for global {@link Category}. */
public interface CategoryRepository extends JpaRepository<Category, Long> {

  List<Category> findAllByOrderBySortOrderAscNameAsc();
}
