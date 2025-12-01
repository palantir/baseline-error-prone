package com.palantir.baseline.errorprone;

import com.google.common.base.MoreObjects;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.Var;
import com.sun.tools.javac.code.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import org.immutables.value.Generated;

/**
 * Immutable implementation of {@link ConsistentOverrides.ParamEntry}.
 * <p>
 * Use the builder to create immutable instances:
 * {@code ImmutableParamEntry.builder()}.
 */
@Generated(from = "ConsistentOverrides.ParamEntry", generator = "Immutables")
@SuppressWarnings({"all"})
@ParametersAreNonnullByDefault
@javax.annotation.processing.Generated("org.immutables.processor.ProxyProcessor")
@Immutable
@CheckReturnValue
final class ImmutableParamEntry
    implements ConsistentOverrides.ParamEntry {
  private final Type type;
  private final String name;
  private final int index;

  private ImmutableParamEntry(Type type, String name, int index) {
    this.type = type;
    this.name = name;
    this.index = index;
  }

  /**
   * @return The value of the {@code type} attribute
   */
  @Override
  public Type type() {
    return type;
  }

  /**
   * @return The value of the {@code name} attribute
   */
  @Override
  public String name() {
    return name;
  }

  /**
   * @return The value of the {@code index} attribute
   */
  @Override
  public int index() {
    return index;
  }

  /**
   * Copy the current immutable object by setting a value for the {@link ConsistentOverrides.ParamEntry#type() type} attribute.
   * A shallow reference equality check is used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for type
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableParamEntry withType(Type value) {
    if (this.type == value) return this;
    Type newValue = Objects.requireNonNull(value, "type");
    return new ImmutableParamEntry(newValue, this.name, this.index);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link ConsistentOverrides.ParamEntry#name() name} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for name
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableParamEntry withName(String value) {
    String newValue = Objects.requireNonNull(value, "name");
    if (this.name.equals(newValue)) return this;
    return new ImmutableParamEntry(this.type, newValue, this.index);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link ConsistentOverrides.ParamEntry#index() index} attribute.
   * A value equality check is used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for index
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableParamEntry withIndex(int value) {
    if (this.index == value) return this;
    return new ImmutableParamEntry(this.type, this.name, value);
  }

  /**
   * This instance is equal to all instances of {@code ImmutableParamEntry} that have equal attribute values.
   * @return {@code true} if {@code this} is equal to {@code another} instance
   */
  @Override
  public boolean equals(@Nullable Object another) {
    if (this == another) return true;
    return another instanceof ImmutableParamEntry
        && equalTo(0, (ImmutableParamEntry) another);
  }

  private boolean equalTo(int synthetic, ImmutableParamEntry another) {
    return type.equals(another.type)
        && name.equals(another.name)
        && index == another.index;
  }

  /**
   * Computes a hash code from attributes: {@code type}, {@code name}, {@code index}.
   * @return hashCode value
   */
  @Override
  public int hashCode() {
    @Var int h = 5381;
    h += (h << 5) + type.hashCode();
    h += (h << 5) + name.hashCode();
    h += (h << 5) + index;
    return h;
  }

  /**
   * Prints the immutable value {@code ParamEntry} with attribute values.
   * @return A string representation of the value
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper("ParamEntry")
        .omitNullValues()
        .add("type", type)
        .add("name", name)
        .add("index", index)
        .toString();
  }

  /**
   * Creates an immutable copy of a {@link ConsistentOverrides.ParamEntry} value.
   * Uses accessors to get values to initialize the new immutable instance.
   * If an instance is already immutable, it is returned as is.
   * @param instance The instance to copy
   * @return A copied immutable ParamEntry instance
   */
  public static ImmutableParamEntry copyOf(ConsistentOverrides.ParamEntry instance) {
    if (instance instanceof ImmutableParamEntry) {
      return (ImmutableParamEntry) instance;
    }
    return ImmutableParamEntry.builder()
        .from(instance)
        .build();
  }

  /**
   * Creates a builder for {@link ImmutableParamEntry ImmutableParamEntry}.
   * <pre>
   * ImmutableParamEntry.builder()
   *    .type(com.sun.tools.javac.code.Type) // required {@link ConsistentOverrides.ParamEntry#type() type}
   *    .name(String) // required {@link ConsistentOverrides.ParamEntry#name() name}
   *    .index(int) // required {@link ConsistentOverrides.ParamEntry#index() index}
   *    .build();
   * </pre>
   * @return A new ImmutableParamEntry builder
   */
  public static ImmutableParamEntry.Builder builder() {
    return new ImmutableParamEntry.Builder();
  }

  /**
   * Builds instances of type {@link ImmutableParamEntry ImmutableParamEntry}.
   * Initialize attributes and then invoke the {@link #build()} method to create an
   * immutable instance.
   * <p><em>{@code Builder} is not thread-safe and generally should not be stored in a field or collection,
   * but instead used immediately to create instances.</em>
   */
  @Generated(from = "ConsistentOverrides.ParamEntry", generator = "Immutables")
  @NotThreadSafe
  public static final class Builder {
    private static final long INIT_BIT_TYPE = 0x1L;
    private static final long INIT_BIT_NAME = 0x2L;
    private static final long INIT_BIT_INDEX = 0x4L;
    private long initBits = 0x7L;

    private @Nullable Type type;
    private @Nullable String name;
    private int index;

    private Builder() {
    }

    /**
     * Fill a builder with attribute values from the provided {@code ParamEntry} instance.
     * Regular attribute values will be replaced with those from the given instance.
     * Absent optional values will not replace present values.
     * @param instance The instance from which to copy values
     * @return {@code this} builder for use in a chained invocation
     */
    @CanIgnoreReturnValue 
    public final Builder from(ConsistentOverrides.ParamEntry instance) {
      Objects.requireNonNull(instance, "instance");
      this.type(instance.type());
      this.name(instance.name());
      this.index(instance.index());
      return this;
    }

    /**
     * Initializes the value for the {@link ConsistentOverrides.ParamEntry#type() type} attribute.
     * @param type The value for type 
     * @return {@code this} builder for use in a chained invocation
     */
    @CanIgnoreReturnValue 
    public final Builder type(Type type) {
      this.type = Objects.requireNonNull(type, "type");
      initBits &= ~INIT_BIT_TYPE;
      return this;
    }

    /**
     * Initializes the value for the {@link ConsistentOverrides.ParamEntry#name() name} attribute.
     * @param name The value for name 
     * @return {@code this} builder for use in a chained invocation
     */
    @CanIgnoreReturnValue 
    public final Builder name(String name) {
      this.name = Objects.requireNonNull(name, "name");
      initBits &= ~INIT_BIT_NAME;
      return this;
    }

    /**
     * Initializes the value for the {@link ConsistentOverrides.ParamEntry#index() index} attribute.
     * @param index The value for index 
     * @return {@code this} builder for use in a chained invocation
     */
    @CanIgnoreReturnValue 
    public final Builder index(int index) {
      this.index = index;
      initBits &= ~INIT_BIT_INDEX;
      return this;
    }

    /**
     * Builds a new {@link ImmutableParamEntry ImmutableParamEntry}.
     * @return An immutable instance of ParamEntry
     * @throws java.lang.IllegalStateException if any required attributes are missing
     */
    public ImmutableParamEntry build() {
      if (initBits != 0) {
        throw new IllegalStateException(formatRequiredAttributesMessage());
      }
      return new ImmutableParamEntry(type, name, index);
    }

    private String formatRequiredAttributesMessage() {
      List<String> attributes = new ArrayList<>();
      if ((initBits & INIT_BIT_TYPE) != 0) attributes.add("type");
      if ((initBits & INIT_BIT_NAME) != 0) attributes.add("name");
      if ((initBits & INIT_BIT_INDEX) != 0) attributes.add("index");
      return "Cannot build ParamEntry, some of required attributes are not set " + attributes;
    }
  }
}
