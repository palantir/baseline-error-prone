package com.palantir.baseline.errorprone;

import com.google.common.base.MoreObjects;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.Var;
import com.sun.source.tree.ExpressionTree;
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
 * Immutable implementation of {@link CardinalityEqualsZero.EqualsZeroExpression}.
 * <p>
 * Use the builder to create immutable instances:
 * {@code new CardinalityEqualsZero.EqualsZeroExpression.Builder()}.
 */
@Generated(from = "CardinalityEqualsZero.EqualsZeroExpression", generator = "Immutables")
@SuppressWarnings({"all"})
@ParametersAreNonnullByDefault
@javax.annotation.processing.Generated("org.immutables.processor.ProxyProcessor")
@Immutable
@CheckReturnValue
final class ImmutableEqualsZeroExpression
    implements CardinalityEqualsZero.EqualsZeroExpression {
  private final CardinalityEqualsZero.ExpressionType type;
  private final ExpressionTree operand;

  private ImmutableEqualsZeroExpression(
      CardinalityEqualsZero.ExpressionType type,
      ExpressionTree operand) {
    this.type = type;
    this.operand = operand;
  }

  /**
   * @return The value of the {@code type} attribute
   */
  @Override
  public CardinalityEqualsZero.ExpressionType type() {
    return type;
  }

  /**
   * @return The value of the {@code operand} attribute
   */
  @Override
  public ExpressionTree operand() {
    return operand;
  }

  /**
   * Copy the current immutable object by setting a value for the {@link CardinalityEqualsZero.EqualsZeroExpression#type() type} attribute.
   * A value equality check is used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for type
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableEqualsZeroExpression withType(CardinalityEqualsZero.ExpressionType value) {
    CardinalityEqualsZero.ExpressionType newValue = Objects.requireNonNull(value, "type");
    if (this.type == newValue) return this;
    return new ImmutableEqualsZeroExpression(newValue, this.operand);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link CardinalityEqualsZero.EqualsZeroExpression#operand() operand} attribute.
   * A shallow reference equality check is used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for operand
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableEqualsZeroExpression withOperand(ExpressionTree value) {
    if (this.operand == value) return this;
    ExpressionTree newValue = Objects.requireNonNull(value, "operand");
    return new ImmutableEqualsZeroExpression(this.type, newValue);
  }

  /**
   * This instance is equal to all instances of {@code ImmutableEqualsZeroExpression} that have equal attribute values.
   * @return {@code true} if {@code this} is equal to {@code another} instance
   */
  @Override
  public boolean equals(@Nullable Object another) {
    if (this == another) return true;
    return another instanceof ImmutableEqualsZeroExpression
        && equalTo(0, (ImmutableEqualsZeroExpression) another);
  }

  private boolean equalTo(int synthetic, ImmutableEqualsZeroExpression another) {
    return type.equals(another.type)
        && operand.equals(another.operand);
  }

  /**
   * Computes a hash code from attributes: {@code type}, {@code operand}.
   * @return hashCode value
   */
  @Override
  public int hashCode() {
    @Var int h = 5381;
    h += (h << 5) + type.hashCode();
    h += (h << 5) + operand.hashCode();
    return h;
  }

  /**
   * Prints the immutable value {@code EqualsZeroExpression} with attribute values.
   * @return A string representation of the value
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper("EqualsZeroExpression")
        .omitNullValues()
        .add("type", type)
        .add("operand", operand)
        .toString();
  }

  /**
   * Creates an immutable copy of a {@link CardinalityEqualsZero.EqualsZeroExpression} value.
   * Uses accessors to get values to initialize the new immutable instance.
   * If an instance is already immutable, it is returned as is.
   * @param instance The instance to copy
   * @return A copied immutable EqualsZeroExpression instance
   */
  public static ImmutableEqualsZeroExpression copyOf(CardinalityEqualsZero.EqualsZeroExpression instance) {
    if (instance instanceof ImmutableEqualsZeroExpression) {
      return (ImmutableEqualsZeroExpression) instance;
    }
    return new CardinalityEqualsZero.EqualsZeroExpression.Builder()
        .from(instance)
        .build();
  }

  /**
   * Builds instances of type {@link ImmutableEqualsZeroExpression ImmutableEqualsZeroExpression}.
   * Initialize attributes and then invoke the {@link #build()} method to create an
   * immutable instance.
   * <p><em>{@code Builder} is not thread-safe and generally should not be stored in a field or collection,
   * but instead used immediately to create instances.</em>
   */
  @Generated(from = "CardinalityEqualsZero.EqualsZeroExpression", generator = "Immutables")
  @NotThreadSafe
  public static class Builder {
    private static final long INIT_BIT_TYPE = 0x1L;
    private static final long INIT_BIT_OPERAND = 0x2L;
    private long initBits = 0x3L;

    private @Nullable CardinalityEqualsZero.ExpressionType type;
    private @Nullable ExpressionTree operand;

    /**
     * Creates a builder for {@link ImmutableEqualsZeroExpression ImmutableEqualsZeroExpression} instances.
     * <pre>
     * new CardinalityEqualsZero.EqualsZeroExpression.Builder()
     *    .type(com.palantir.baseline.errorprone.CardinalityEqualsZero.ExpressionType) // required {@link CardinalityEqualsZero.EqualsZeroExpression#type() type}
     *    .operand(com.sun.source.tree.ExpressionTree) // required {@link CardinalityEqualsZero.EqualsZeroExpression#operand() operand}
     *    .build();
     * </pre>
     */
    public Builder() {
      if (!(this instanceof CardinalityEqualsZero.EqualsZeroExpression.Builder)) {
        throw new UnsupportedOperationException("Use: new CardinalityEqualsZero.EqualsZeroExpression.Builder()");
      }
    }

    /**
     * Fill a builder with attribute values from the provided {@code EqualsZeroExpression} instance.
     * Regular attribute values will be replaced with those from the given instance.
     * Absent optional values will not replace present values.
     * @param instance The instance from which to copy values
     * @return {@code this} builder for use in a chained invocation
     */
    @CanIgnoreReturnValue 
    public final CardinalityEqualsZero.EqualsZeroExpression.Builder from(CardinalityEqualsZero.EqualsZeroExpression instance) {
      Objects.requireNonNull(instance, "instance");
      this.type(instance.type());
      this.operand(instance.operand());
      return (CardinalityEqualsZero.EqualsZeroExpression.Builder) this;
    }

    /**
     * Initializes the value for the {@link CardinalityEqualsZero.EqualsZeroExpression#type() type} attribute.
     * @param type The value for type 
     * @return {@code this} builder for use in a chained invocation
     */
    @CanIgnoreReturnValue 
    public final CardinalityEqualsZero.EqualsZeroExpression.Builder type(CardinalityEqualsZero.ExpressionType type) {
      this.type = Objects.requireNonNull(type, "type");
      initBits &= ~INIT_BIT_TYPE;
      return (CardinalityEqualsZero.EqualsZeroExpression.Builder) this;
    }

    /**
     * Initializes the value for the {@link CardinalityEqualsZero.EqualsZeroExpression#operand() operand} attribute.
     * @param operand The value for operand 
     * @return {@code this} builder for use in a chained invocation
     */
    @CanIgnoreReturnValue 
    public final CardinalityEqualsZero.EqualsZeroExpression.Builder operand(ExpressionTree operand) {
      this.operand = Objects.requireNonNull(operand, "operand");
      initBits &= ~INIT_BIT_OPERAND;
      return (CardinalityEqualsZero.EqualsZeroExpression.Builder) this;
    }

    /**
     * Builds a new {@link ImmutableEqualsZeroExpression ImmutableEqualsZeroExpression}.
     * @return An immutable instance of EqualsZeroExpression
     * @throws java.lang.IllegalStateException if any required attributes are missing
     */
    public ImmutableEqualsZeroExpression build() {
      if (initBits != 0) {
        throw new IllegalStateException(formatRequiredAttributesMessage());
      }
      return new ImmutableEqualsZeroExpression(type, operand);
    }

    private String formatRequiredAttributesMessage() {
      List<String> attributes = new ArrayList<>();
      if ((initBits & INIT_BIT_TYPE) != 0) attributes.add("type");
      if ((initBits & INIT_BIT_OPERAND) != 0) attributes.add("operand");
      return "Cannot build EqualsZeroExpression, some of required attributes are not set " + attributes;
    }
  }
}
