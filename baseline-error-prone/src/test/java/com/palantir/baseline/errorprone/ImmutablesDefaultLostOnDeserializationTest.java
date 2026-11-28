/*
 * (c) Copyright 2026 Palantir Technologies Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.palantir.baseline.errorprone;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.jupiter.api.Test;

@SuppressWarnings("MisformattedTestData")
public class ImmutablesDefaultLostOnDeserializationTest {

    @Test
    public void subtypeViaImmutableBuilder() {
        helper().addSourceLines(
                        "InstallConfig.java",
                        // language=Java
                        """
                        import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
                        import org.immutables.value.Value;
                        @Value.Immutable
                        @JsonDeserialize(as = ImmutableInstallConfig.class)
                        interface InstallConfig extends BaseInstallConfig {
                            @Override
                            @Value.Default
                            // BUG: Diagnostic contains: This default returns CustomServerConfig, but Jackson deserializes the property as ServerConfig (via @JsonDeserialize(as = ImmutableServerConfig.class)), so CustomServerConfig's defaults are lost whenever the property is present. Annotate the method with @JsonDeserialize(as = CustomServerConfig.class)
                            default ServerConfig server() {
                                return ImmutableCustomServerConfig.builder().build();
                            }
                        }
                        """)
                .doTest();
    }

    @Test
    public void subtypeViaFactory() {
        helper().addSourceLines(
                        "InstallConfig.java",
                        // language=Java
                        """
                        import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
                        import org.immutables.value.Value;
                        @Value.Immutable
                        @JsonDeserialize(as = ImmutableInstallConfig.class)
                        interface InstallConfig extends BaseInstallConfig {
                            @Override
                            @Value.Default
                            // BUG: Diagnostic contains: This default returns CustomServerConfig
                            default ServerConfig server() {
                                return CustomServerConfig.of();
                            }
                        }
                        """)
                .doTest();
    }

    @Test
    public void subtypeViaNewBuilder() {
        helper().addSourceLines(
                        "InstallConfig.java",
                        // language=Java
                        """
                        import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
                        import org.immutables.value.Value;
                        @Value.Immutable
                        @JsonDeserialize(as = ImmutableInstallConfig.class)
                        interface InstallConfig extends BaseInstallConfig {
                            @Override
                            @Value.Default
                            // BUG: Diagnostic contains: This default returns CustomServerConfig
                            default ServerConfig server() {
                                return new CustomServerConfig.Builder().build();
                            }
                        }
                        """)
                .doTest();
    }

    @Test
    public void subtypeInAbstractClass() {
        helper().addSourceLines(
                        "InstallConfig.java",
                        // language=Java
                        """
                        import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
                        import org.immutables.value.Value;
                        @Value.Immutable
                        @JsonDeserialize(as = ImmutableInstallConfig.class)
                        abstract class InstallConfig {
                            @Value.Default
                            // BUG: Diagnostic contains: This default returns CustomServerConfig
                            public ServerConfig server() {
                                return CustomServerConfig.of();
                            }
                        }
                        """)
                .doTest();
    }

    @Test
    public void subtypeWithoutJsonDeserialize() {
        helper().addSourceLines(
                        "PlainServerConfig.java",
                        // language=Java
                        """
                        interface PlainServerConfig extends ServerConfig {
                            static PlainServerConfig of() {
                                return null;
                            }
                        }
                        """)
                .addSourceLines(
                        "InstallConfig.java",
                        // language=Java
                        """
                        import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
                        import org.immutables.value.Value;
                        @Value.Immutable
                        @JsonDeserialize(as = ImmutableInstallConfig.class)
                        interface InstallConfig extends BaseInstallConfig {
                            @Override
                            @Value.Default
                            // BUG: Diagnostic contains: Annotate PlainServerConfig with @JsonDeserialize(as = <its implementation>.class), then annotate the method with @JsonDeserialize(as = PlainServerConfig.class)
                            default ServerConfig server() {
                                return PlainServerConfig.of();
                            }
                        }
                        """)
                .doTest();
    }

    @Test
    public void customisedBaseBuilder() {
        helper().addSourceLines(
                        "InstallConfig.java",
                        // language=Java
                        """
                        import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
                        import org.immutables.value.Value;
                        @Value.Immutable
                        @JsonDeserialize(as = ImmutableInstallConfig.class)
                        interface InstallConfig extends BaseInstallConfig {
                            @Override
                            @Value.Default
                            // BUG: Diagnostic contains: This default customises ServerConfig using its builder, but the customised values only apply when the property is absent
                            default ServerConfig server() {
                                return ServerConfig.builder().gzip(false).build();
                            }
                        }
                        """)
                .doTest();
    }

    @Test
    public void customisedImmutableBuilder() {
        helper().addSourceLines(
                        "InstallConfig.java",
                        // language=Java
                        """
                        import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
                        import org.immutables.value.Value;
                        @Value.Immutable
                        @JsonDeserialize(as = ImmutableInstallConfig.class)
                        interface InstallConfig extends BaseInstallConfig {
                            @Override
                            @Value.Default
                            // BUG: Diagnostic contains: This default customises ServerConfig
                            default ServerConfig server() {
                                return ImmutableServerConfig.builder().maxThreads(10).build();
                            }
                        }
                        """)
                .doTest();
    }

    @Test
    public void customisedNewBuilder() {
        helper().addSourceLines(
                        "InstallConfig.java",
                        // language=Java
                        """
                        import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
                        import org.immutables.value.Value;
                        @Value.Immutable
                        @JsonDeserialize(as = ImmutableInstallConfig.class)
                        interface InstallConfig extends BaseInstallConfig {
                            @Override
                            @Value.Default
                            // BUG: Diagnostic contains: This default customises ServerConfig
                            default ServerConfig server() {
                                return new ServerConfig.Builder().gzip(false).build();
                            }
                        }
                        """)
                .doTest();
    }

    @Test
    public void customisedBuilderFromSuper() {
        helper().addSourceLines(
                        "InstallConfig.java",
                        // language=Java
                        """
                        import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
                        import org.immutables.value.Value;
                        @Value.Immutable
                        @JsonDeserialize(as = ImmutableInstallConfig.class)
                        interface InstallConfig extends BaseInstallConfig {
                            @Override
                            @Value.Default
                            // BUG: Diagnostic contains: This default customises ServerConfig
                            default ServerConfig server() {
                                return ServerConfig.builder()
                                        .from(BaseInstallConfig.super.server())
                                        .gzip(false)
                                        .build();
                            }
                        }
                        """)
                .doTest();
    }

    @Test
    public void customisedBuilderAfterLocal() {
        helper().addSourceLines(
                        "InstallConfig.java",
                        // language=Java
                        """
                        import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
                        import org.immutables.value.Value;
                        @Value.Immutable
                        @JsonDeserialize(as = ImmutableInstallConfig.class)
                        interface InstallConfig extends BaseInstallConfig {
                            @Override
                            @Value.Default
                            // BUG: Diagnostic contains: This default customises ServerConfig
                            default ServerConfig server() {
                                ServerConfig base = ServerConfig.builder().build();
                                return ServerConfig.builder()
                                        .maxThreads(Math.max(512, 32 * base.maxThreads()))
                                        .build();
                            }
                        }
                        """)
                .doTest();
    }

    @Test
    public void baseWithoutSetters() {
        helper().addSourceLines(
                        "InstallConfig.java",
                        // language=Java
                        """
                        import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
                        import org.immutables.value.Value;
                        @Value.Immutable
                        @JsonDeserialize(as = ImmutableInstallConfig.class)
                        interface InstallConfig {
                            @Value.Default
                            default ServerConfig server() {
                                return ServerConfig.of();
                            }

                            @Value.Default
                            default ServerConfig admin() {
                                return ImmutableServerConfig.builder().build();
                            }

                            @Value.Default
                            default ServerConfig management() {
                                return new ServerConfig.Builder().build();
                            }

                            @Value.Default
                            default ServerConfig copy() {
                                return ServerConfig.builder().from(admin()).build();
                            }
                        }
                        """)
                .doTest();
    }

    @Test
    public void declaredTypeIsSubtype() {
        helper().addSourceLines(
                        "InstallConfig.java",
                        // language=Java
                        """
                        import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
                        import org.immutables.value.Value;
                        @Value.Immutable
                        @JsonDeserialize(as = ImmutableInstallConfig.class)
                        interface InstallConfig {
                            @Value.Default
                            default CustomServerConfig server() {
                                return ImmutableCustomServerConfig.builder().build();
                            }
                        }
                        """)
                .doTest();
    }

    @Test
    public void methodAlreadyAnnotated() {
        helper().addSourceLines(
                        "InstallConfig.java",
                        // language=Java
                        """
                        import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
                        import org.immutables.value.Value;
                        @Value.Immutable
                        @JsonDeserialize(as = ImmutableInstallConfig.class)
                        interface InstallConfig extends BaseInstallConfig {
                            @Override
                            @Value.Default
                            @JsonDeserialize(as = ImmutableCustomServerConfig.class)
                            default ServerConfig server() {
                                return CustomServerConfig.of();
                            }
                        }
                        """)
                .doTest();
    }

    @Test
    public void polymorphicDeclaredType() {
        helper().addSourceLines(
                        "Auth.java",
                        // language=Java
                        """
                        import com.fasterxml.jackson.annotation.JsonSubTypes;
                        import com.fasterxml.jackson.annotation.JsonTypeInfo;
                        import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
                        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
                        @JsonSubTypes(@JsonSubTypes.Type(value = TokenAuth.class, name = "token"))
                        interface Auth {}
                        @JsonDeserialize(as = TokenAuth.class)
                        interface TaggedAuth extends Auth {}
                        final class TokenAuth implements TaggedAuth {}
                        final class OtherAuth implements TaggedAuth {}
                        """)
                .addSourceLines(
                        "InstallConfig.java",
                        // language=Java
                        """
                        import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
                        import org.immutables.value.Value;
                        @Value.Immutable
                        @JsonDeserialize(as = ImmutableInstallConfig.class)
                        interface InstallConfig {
                            @Value.Default
                            default TaggedAuth auth() {
                                return new OtherAuth();
                            }
                        }
                        """)
                .doTest();
    }

    @Test
    public void jsonSubTypesDeclaredType() {
        helper().addSourceLines(
                        "Auth.java",
                        // language=Java
                        """
                        import com.fasterxml.jackson.annotation.JsonSubTypes;
                        import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
                        @JsonSubTypes(@JsonSubTypes.Type(TokenAuth.class))
                        @JsonDeserialize(as = TokenAuth.class)
                        interface Auth {}
                        final class TokenAuth implements Auth {}
                        final class OtherAuth implements Auth {}
                        """)
                .addSourceLines(
                        "InstallConfig.java",
                        // language=Java
                        """
                        import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
                        import org.immutables.value.Value;
                        @Value.Immutable
                        @JsonDeserialize(as = ImmutableInstallConfig.class)
                        interface InstallConfig {
                            @Value.Default
                            default Auth auth() {
                                return new OtherAuth();
                            }
                        }
                        """)
                .doTest();
    }

    @Test
    public void ignoredDerivedAndLazy() {
        helper().addSourceLines(
                        "InstallConfig.java",
                        // language=Java
                        """
                        import com.fasterxml.jackson.annotation.JsonIgnore;
                        import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
                        import org.immutables.value.Value;
                        @Value.Immutable
                        @JsonDeserialize(as = ImmutableInstallConfig.class)
                        interface InstallConfig {
                            @JsonIgnore
                            @Value.Default
                            default ServerConfig server() {
                                return CustomServerConfig.of();
                            }

                            @Value.Derived
                            default ServerConfig derived() {
                                return CustomServerConfig.of();
                            }

                            @Value.Lazy
                            default ServerConfig lazy() {
                                return ServerConfig.builder().gzip(false).build();
                            }
                        }
                        """)
                .doTest();
    }

    @Test
    public void enclosingTypeNotDeserialized() {
        helper().addSourceLines(
                        "InstallConfig.java",
                        // language=Java
                        """
                        import org.immutables.value.Value;
                        @Value.Immutable
                        interface InstallConfig {
                            @Value.Default
                            default ServerConfig server() {
                                return CustomServerConfig.of();
                            }
                        }
                        """)
                .doTest();
    }

    @Test
    public void declaredTypeNotDeserialized() {
        helper().addSourceLines(
                        "Plain.java",
                        // language=Java
                        """
                        interface Plain {}
                        final class PlainImpl implements Plain {}
                        """)
                .addSourceLines(
                        "InstallConfig.java",
                        // language=Java
                        """
                        import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
                        import org.immutables.value.Value;
                        @Value.Immutable
                        @JsonDeserialize(as = ImmutableInstallConfig.class)
                        interface InstallConfig {
                            @Value.Default
                            default Plain plain() {
                                return new PlainImpl();
                            }
                        }
                        """)
                .doTest();
    }

    @Test
    public void containerTypes() {
        helper().addSourceLines(
                        "InstallConfig.java",
                        // language=Java
                        """
                        import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
                        import java.util.List;
                        import java.util.Map;
                        import java.util.Optional;
                        import org.immutables.value.Value;
                        @Value.Immutable
                        @JsonDeserialize(as = ImmutableInstallConfig.class)
                        interface InstallConfig {
                            @Value.Default
                            default Optional<ServerConfig> server() {
                                return Optional.of(CustomServerConfig.of());
                            }

                            @Value.Default
                            default List<ServerConfig> servers() {
                                return List.of(CustomServerConfig.of());
                            }

                            @Value.Default
                            default Map<String, ServerConfig> serversByName() {
                                return Map.of("a", CustomServerConfig.of());
                            }

                            @Value.Default
                            default String name() {
                                return "name";
                            }
                        }
                        """)
                .doTest();
    }

    @Test
    public void subtypeOnlyInLambdaOrAnonymousClass() {
        helper().addSourceLines(
                        "InstallConfig.java",
                        // language=Java
                        """
                        import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
                        import java.util.function.Supplier;
                        import org.immutables.value.Value;
                        @Value.Immutable
                        @JsonDeserialize(as = ImmutableInstallConfig.class)
                        interface InstallConfig {
                            @Value.Default
                            default ServerConfig server() {
                                Supplier<ServerConfig> lambda = () -> {
                                    return CustomServerConfig.of();
                                };
                                Supplier<ServerConfig> anonymous = new Supplier<ServerConfig>() {
                                    @Override
                                    public ServerConfig get() {
                                        return ServerConfig.builder().gzip(false).build();
                                    }
                                };
                                return ServerConfig.of();
                            }
                        }
                        """)
                .doTest();
    }

    @Test
    public void fixAddsJsonDeserialize() {
        fix().addInputLines(
                        "InstallConfig.java",
                        // language=Java
                        """
                        import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
                        import org.immutables.value.Value;
                        @Value.Immutable
                        @JsonDeserialize(as = ImmutableInstallConfig.class)
                        interface InstallConfig extends BaseInstallConfig {
                            @Override
                            @Value.Default
                            default ServerConfig server() {
                                return CustomServerConfig.of();
                            }
                        }
                        """)
                .addOutputLines(
                        "InstallConfig.java",
                        // language=Java
                        """
                        import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
                        import org.immutables.value.Value;
                        @Value.Immutable
                        @JsonDeserialize(as = ImmutableInstallConfig.class)
                        interface InstallConfig extends BaseInstallConfig {
                            @Override
                            @Value.Default
                            @JsonDeserialize(as = CustomServerConfig.class)
                            default ServerConfig server() {
                                return CustomServerConfig.of();
                            }
                        }
                        """)
                .doTest();
    }

    @Test
    public void fixAddsJsonDeserializeImport() {
        fix().addInputLines(
                        "BaseConfig.java",
                        // language=Java
                        """
                        import org.immutables.value.Value;
                        @Value.Immutable
                        @com.fasterxml.jackson.databind.annotation.JsonDeserialize(as = ImmutableInstallConfig.class)
                        abstract class BaseConfig {
                            @Value.Default
                            public ServerConfig server() {
                                return ImmutableCustomServerConfig.builder().build();
                            }
                        }
                        """)
                .addOutputLines(
                        "BaseConfig.java",
                        // language=Java
                        """
                        import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
                        import org.immutables.value.Value;
                        @Value.Immutable
                        @com.fasterxml.jackson.databind.annotation.JsonDeserialize(as = ImmutableInstallConfig.class)
                        abstract class BaseConfig {
                            @Value.Default
                            @JsonDeserialize(as = CustomServerConfig.class)
                            public ServerConfig server() {
                                return ImmutableCustomServerConfig.builder().build();
                            }
                        }
                        """)
                .doTest();
    }

    private CompilationTestHelper helper() {
        CompilationTestHelper helper =
                CompilationTestHelper.newInstance(ImmutablesDefaultLostOnDeserialization.class, getClass());
        for (String[] stub : STUBS) {
            helper = helper.addSourceLines(stub[0], stub[1]);
        }
        return helper;
    }

    private BugCheckerRefactoringTestHelper fix() {
        BugCheckerRefactoringTestHelper helper =
                BugCheckerRefactoringTestHelper.newInstance(ImmutablesDefaultLostOnDeserialization.class, getClass());
        for (String[] stub : STUBS) {
            helper = helper.addInputLines(stub[0], stub[1]).expectUnchanged();
        }
        return helper;
    }

    /**
     * Hand-written stand-ins for the classes the Immutables annotation processor would generate, which isn't run on
     * test inputs.
     */
    private static final String[][] STUBS = {
        {"ServerConfig.java", """
            import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
            import org.immutables.value.Value;
            @Value.Immutable
            @JsonDeserialize(as = ImmutableServerConfig.class)
            interface ServerConfig {
                @Value.Default
                default boolean gzip() {
                    return true;
                }

                @Value.Default
                default int maxThreads() {
                    return 100;
                }

                static ServerConfig of() {
                    return builder().build();
                }

                static ImmutableServerConfig.Builder builder() {
                    return ImmutableServerConfig.builder();
                }

                final class Builder extends ImmutableServerConfig.Builder {}
            }
            """},
        {"ImmutableServerConfig.java", """
            final class ImmutableServerConfig implements ServerConfig {
                static Builder builder() {
                    return new Builder();
                }

                static class Builder {
                    Builder from(ServerConfig instance) {
                        return this;
                    }

                    Builder gzip(boolean gzip) {
                        return this;
                    }

                    Builder maxThreads(int maxThreads) {
                        return this;
                    }

                    ImmutableServerConfig build() {
                        return new ImmutableServerConfig();
                    }
                }
            }
            """},
        {"CustomServerConfig.java", """
            import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
            import org.immutables.value.Value;
            @Value.Immutable
            @JsonDeserialize(as = ImmutableCustomServerConfig.class)
            interface CustomServerConfig extends ServerConfig {
                @Override
                @Value.Default
                default boolean gzip() {
                    return false;
                }

                static CustomServerConfig of() {
                    return ImmutableCustomServerConfig.builder().build();
                }

                final class Builder extends ImmutableCustomServerConfig.Builder {}
            }
            """},
        {"ImmutableCustomServerConfig.java", """
            final class ImmutableCustomServerConfig implements CustomServerConfig {
                static Builder builder() {
                    return new Builder();
                }

                static class Builder {
                    ImmutableCustomServerConfig build() {
                        return new ImmutableCustomServerConfig();
                    }
                }
            }
            """},
        {"BaseInstallConfig.java", """
            import org.immutables.value.Value;
            interface BaseInstallConfig {
                @Value.Default
                default ServerConfig server() {
                    return ServerConfig.of();
                }
            }
            """},
        {"ImmutableInstallConfig.java", """
            final class ImmutableInstallConfig {}
            """},
    };
}
