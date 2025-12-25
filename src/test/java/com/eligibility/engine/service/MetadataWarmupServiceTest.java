package com.eligibility.engine.service;

import com.eligibility.engine.model.AttributeDef;
import com.eligibility.engine.repository.CachedMetadata;
import com.eligibility.engine.repository.MetadataRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MetadataWarmupServiceTest {

    @Test
    void warmup_usesPersistentCache_andSkipsRemoteDiscovery() throws Exception {
        MetadataRepository repo = mock(MetadataRepository.class);
        MockExternalDiscoveryService remote = mock(MockExternalDiscoveryService.class);
        SchemaServiceImpl schemaService = mock(SchemaServiceImpl.class);

        ObjectMapper mapper = new ObjectMapper();
        Map<String, AttributeDef> schema = Map.of(
                "income", new AttributeDef("income", "Integer", Set.of(">"))
        );
        Set<String> lists = Set.of("premium_users");

        when(repo.findById("SCHEMA")).thenReturn(Optional.of(new CachedMetadata("SCHEMA", mapper.writeValueAsString(schema), 1L)));
        when(repo.findById("LISTS")).thenReturn(Optional.of(new CachedMetadata("LISTS", mapper.writeValueAsString(lists), 1L)));

        MetadataWarmupService svc = new MetadataWarmupService(repo, remote, schemaService);
        svc.init();

        verify(remote, never()).fetchSchemaFromRemote();
        verify(remote, never()).fetchListsFromRemote();
        verify(schemaService, times(1)).applySnapshot(anyMap(), anySet());
    }

    @Test
    void warmup_fetchesRemote_whenCacheMissing_andPersists() throws Exception {
        MetadataRepository repo = mock(MetadataRepository.class);
        MockExternalDiscoveryService remote = mock(MockExternalDiscoveryService.class);
        SchemaServiceImpl schemaService = mock(SchemaServiceImpl.class);

        when(repo.findById("SCHEMA")).thenReturn(Optional.empty());
        when(repo.findById("LISTS")).thenReturn(Optional.empty());

        Map<String, AttributeDef> schema = Map.of(
                "income", new AttributeDef("income", "Integer", Set.of(">"))
        );
        Set<String> lists = Set.of("premium_users");

        when(remote.fetchSchemaFromRemote()).thenReturn(schema);
        when(remote.fetchListsFromRemote()).thenReturn(lists);

        MetadataWarmupService svc = new MetadataWarmupService(repo, remote, schemaService);
        svc.init();

        verify(remote, times(1)).fetchSchemaFromRemote();
        verify(remote, times(1)).fetchListsFromRemote();

        // Verify it persisted both entries
        ArgumentCaptor<CachedMetadata> cap = ArgumentCaptor.forClass(CachedMetadata.class);
        verify(repo, atLeast(2)).save(cap.capture());

        List<String> ids = cap.getAllValues().stream().map(m -> m.id).toList();
        assertTrue(ids.contains("SCHEMA"));
        assertTrue(ids.contains("LISTS"));

        verify(schemaService, times(1)).applySnapshot(anyMap(), anySet());
    }
}