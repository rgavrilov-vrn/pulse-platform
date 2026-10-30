package ru.agroeco.pulse.incident.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.incident.adapter.http.dto.CategoryGroupView;
import ru.agroeco.pulse.incident.adapter.http.dto.CategoryView;
import ru.agroeco.pulse.incident.adapter.http.dto.ChecklistItemView;
import ru.agroeco.pulse.incident.adapter.persistence.CategoryGroupRepository;
import ru.agroeco.pulse.incident.adapter.persistence.ChecklistItemRepository;
import ru.agroeco.pulse.incident.adapter.persistence.ViolationCategoryRepository;
import ru.agroeco.pulse.incident.domain.CategoryGroupEntity;
import ru.agroeco.pulse.incident.domain.ChecklistItemEntity;
import ru.agroeco.pulse.incident.domain.ViolationCategoryEntity;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IncidentCatalogService {

    private final CategoryGroupRepository groups;
    private final ViolationCategoryRepository categories;
    private final ChecklistItemRepository checklistItems;

    public List<CategoryGroupView> listGroups() {
        return groups.findAllByActiveTrueOrderBySortOrderAscCodeAsc()
                .stream().map(this::toGroupView).toList();
    }

    public List<CategoryView> listCategories(UUID groupId) {
        List<ViolationCategoryEntity> rows = (groupId == null)
                ? categories.findAllActiveOrdered()
                : categories.findAllByActiveTrueAndGroupIdOrderBySortOrderAscCodeAsc(groupId);
        return rows.stream().map(this::toCategoryView).toList();
    }

    public List<ChecklistItemView> listChecklistItems(UUID categoryId) {
        List<ChecklistItemEntity> rows = (categoryId == null)
                ? checklistItems.findAllByActiveTrueOrderBySortOrderAscCodeAsc()
                : checklistItems.findAllByActiveTrueAndCategoryIdOrderBySortOrderAscCodeAsc(categoryId);
        return rows.stream().map(this::toChecklistItemView).toList();
    }

    private CategoryGroupView toGroupView(CategoryGroupEntity g) {
        return new CategoryGroupView(g.getId(), g.getCode(), g.getName(),
                g.getIcon(), g.getSortOrder());
    }

    private CategoryView toCategoryView(ViolationCategoryEntity c) {
        return new CategoryView(c.getId(), c.getGroupId(), c.getCode(), c.getName());
    }

    private ChecklistItemView toChecklistItemView(ChecklistItemEntity i) {
        return new ChecklistItemView(i.getId(), i.getCategoryId(),
                i.getName(), i.getCommentTemplate());
    }
}