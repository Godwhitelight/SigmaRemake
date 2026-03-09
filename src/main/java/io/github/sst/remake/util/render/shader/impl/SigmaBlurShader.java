package io.github.sst.remake.util.render.shader.impl;

import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * SigmaBlurShader - custom ResourceManager that intercepts "jelloblur" resource requests.
 *
 * In 1.19+, the ResourceManager interface changed significantly:
 * - getResource returns Optional<Resource> instead of Resource
 * - containsResource was removed
 * - getAllResources signature changed
 * - findResources returns Map<Identifier, Resource> instead of Collection<Identifier>
 *
 * TODO: This class was used with ShaderEffect which is removed in 1.21.
 * Once PostEffectProcessor-based blur is implemented, this may need further adaptation
 * or may become unnecessary entirely.
 */
public class SigmaBlurShader implements ResourceManager {
    @Override
    public Set<String> getAllNamespaces() {
        return MinecraftClient.getInstance().getResourceManager().getAllNamespaces();
    }

    @Override
    public Optional<Resource> getResource(Identifier id) {
        // In the old code, this returned JelloBlurJSON for "jelloblur" path.
        // Since Resource is now a class (not interface), JelloBlurJSON can't be returned directly.
        // Delegate to default resource manager for now.
        return MinecraftClient.getInstance().getResourceManager().getResource(id);
    }

    @Override
    public List<Resource> getAllResources(Identifier id) {
        return MinecraftClient.getInstance().getResourceManager().getAllResources(id);
    }

    @Override
    public Map<Identifier, Resource> findResources(String startingPath, Predicate<Identifier> pathPredicate) {
        return MinecraftClient.getInstance().getResourceManager().findResources(startingPath, pathPredicate);
    }

    @Override
    public Map<Identifier, List<Resource>> findAllResources(String startingPath, Predicate<Identifier> pathPredicate) {
        return MinecraftClient.getInstance().getResourceManager().findAllResources(startingPath, pathPredicate);
    }

    @Override
    public Stream<net.minecraft.resource.ResourcePack> streamResourcePacks() {
        return Stream.empty();
    }
}
