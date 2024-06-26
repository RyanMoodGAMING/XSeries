package com.cryptomorin.xseries.reflection.jvm.classes;

import com.cryptomorin.xseries.reflection.XReflection;
import com.cryptomorin.xseries.reflection.jvm.ReflectiveNamespace;
import com.google.common.base.Strings;
import org.intellij.lang.annotations.Pattern;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class DynamicClassHandle extends ClassHandle {
    protected ClassHandle parent;
    protected String packageName;
    protected final Set<String> classNames = new HashSet<>(5);
    protected int array;

    public DynamicClassHandle(ReflectiveNamespace namespace) {
        super(namespace);
    }

    public DynamicClassHandle inPackage(@Pattern(PackageHandle.JAVA_PACKAGE_PATTERN) String packageName) {
        Objects.requireNonNull(packageName, "Null package name");
        this.packageName = packageName;
        return this;
    }

    public DynamicClassHandle inPackage(PackageHandle packageHandle) {
        return inPackage(packageHandle, "");
    }

    public DynamicClassHandle inPackage(PackageHandle packageHandle, @Pattern(PackageHandle.JAVA_PACKAGE_PATTERN) String packageName) {
        Objects.requireNonNull(packageHandle, "Null package handle type");
        Objects.requireNonNull(packageName, "Null package handle name");
        if (parent != null)
            throw new IllegalStateException("Cannot change package of an inner class: " + packageHandle + " -> " + packageName);
        this.packageName = packageHandle.getPackage(packageName);
        return this;
    }

    public DynamicClassHandle named(@Pattern(PackageHandle.JAVA_IDENTIFIER_PATTERN) String... classNames) {
        Objects.requireNonNull(classNames);
        for (String className : this.classNames) {
            Objects.requireNonNull(className, () -> "Cannot add null class name from: " + Arrays.toString(classNames) + " to " + this);
        }
        this.classNames.addAll(Arrays.asList(classNames));
        return this;
    }

    public String[] reflectClassNames() {
        if (this.parent == null) Objects.requireNonNull(packageName, "Package name is null");
        String[] classNames = new String[this.classNames.size()];
        Class<?> parent = this.parent == null ? null : XReflection.of(this.parent.unreflect()).asArray(0).unreflect();

        int i = 0;
        for (String className : this.classNames) {

            @SuppressWarnings("NonConstantStringShouldBeStringBuffer")
            String clazz;
            if (parent == null) clazz = packageName + '.' + className;
            else clazz = parent.getName() + '$' + className;

            if (array != 0) clazz = Strings.repeat("[", array) + 'L' + clazz + ';';
            classNames[i++] = clazz;
        }

        return classNames;
    }

    @Override
    public Class<?> reflect() throws ClassNotFoundException {
        ClassNotFoundException errors = null;

        for (String className : reflectClassNames()) {
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException ex) {
                if (errors == null) errors = new ClassNotFoundException("None of the classes were found");
                errors.addSuppressed(ex);
            }
        }

        throw XReflection.relativizeSuppressedExceptions(errors);
    }

    @Override
    public DynamicClassHandle asArray(int dimension) {
        if (dimension < 0) throw new IllegalArgumentException("Array dimension cannot be negative: " + dimension);
        this.array = dimension;
        return this;
    }

    @Override
    public boolean isArray() {
        return this.array > 0;
    }

    @Override
    public Set<String> getPossibleNames() {
        return classNames;
    }
}
