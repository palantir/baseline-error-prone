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

import com.google.errorprone.CompilationTestHelper;
import org.junit.jupiter.api.Test;

@SuppressWarnings("MisformattedTestData")
public class ImmutablesDefaultOverrideMissingAnnotationTest {

    private static final String SERVER_CONFIG =
            // language=Java
            """
            import org.immutables.value.Value;
            interface BaseServerConfig {
                @Value.Default
                default int maxThreads() {
                    return 100;
                }

                default int processors() {
                    return 4;
                }

                int port();
            }
            """;

    @Test
    public void overrideWithoutDefault() {
        helper().addSourceLines("BaseServerConfig.java", SERVER_CONFIG)
                .addSourceLines(
                        "ServerConfig.java",
                        // language=Java
                        """
                        import com.fasterxml.jackson.annotation.JsonProperty;
                        import org.immutables.value.Value;
                        @Value.Immutable
                        interface ServerConfig extends BaseServerConfig {
                            @Override
                            @JsonProperty("max-threads")
                            // BUG: Diagnostic contains: so Immutables no longer treats it as an attribute
                            default int maxThreads() {
                                return 10;
                            }
                        }
                        """)
                .doTest();
    }

    @Test
    public void overrideInAbstractClass() {
        helper().addSourceLines("BaseServerConfig.java", SERVER_CONFIG)
                .addSourceLines(
                        "ServerConfig.java",
                        // language=Java
                        """
                        import org.immutables.value.Value;
                        @Value.Immutable
                        abstract class ServerConfig implements BaseServerConfig {
                            @Override
                            // BUG: Diagnostic contains: ImmutablesDefaultOverrideMissingAnnotation
                            public int maxThreads() {
                                return 10;
                            }
                        }
                        """)
                .doTest();
    }

    @Test
    public void annotatedOverrides() {
        helper().addSourceLines("BaseServerConfig.java", SERVER_CONFIG)
                .addSourceLines(
                        "ServerConfig.java",
                        // language=Java
                        """
                        import org.immutables.value.Value;
                        @Value.Immutable
                        interface ServerConfig extends BaseServerConfig {
                            @Override
                            @Value.Default
                            default int maxThreads() {
                                return 10;
                            }
                        }
                        """)
                .addSourceLines(
                        "DerivedServerConfig.java",
                        // language=Java
                        """
                        import org.immutables.value.Value;
                        @Value.Immutable
                        interface DerivedServerConfig extends BaseServerConfig {
                            @Override
                            @Value.Derived
                            default int maxThreads() {
                                return 10;
                            }
                        }
                        """)
                .doTest();
    }

    @Test
    public void overridesOfNonDefaults() {
        helper().addSourceLines("BaseServerConfig.java", SERVER_CONFIG)
                .addSourceLines(
                        "ServerConfig.java",
                        // language=Java
                        """
                        import org.immutables.value.Value;
                        @Value.Immutable
                        interface ServerConfig extends BaseServerConfig {
                            @Override
                            default int processors() {
                                return 8;
                            }

                            @Override
                            default int port() {
                                return 8443;
                            }
                        }
                        """)
                .doTest();
    }

    @Test
    public void notImmutable() {
        helper().addSourceLines("BaseServerConfig.java", SERVER_CONFIG)
                .addSourceLines(
                        "ServerConfig.java",
                        // language=Java
                        """
                        interface ServerConfig extends BaseServerConfig {
                            @Override
                            default int maxThreads() {
                                return 10;
                            }
                        }
                        """)
                .doTest();
    }

    @Test
    public void fixAddsDefault() {
        RefactoringValidator.of(ImmutablesDefaultOverrideMissingAnnotation.class, getClass())
                .addInputLines(
                        "ServerConfig.java",
                        // language=Java
                        """
                        import org.immutables.value.Value;
                        interface ServerConfig {
                            @Value.Default
                            default int maxThreads() {
                                return 100;
                            }

                            @Value.Immutable
                            interface Custom extends ServerConfig {
                                @Override
                                default int maxThreads() {
                                    return 10;
                                }
                            }
                        }
                        """)
                .addOutputLines(
                        "ServerConfig.java",
                        // language=Java
                        """
                        import org.immutables.value.Value;
                        interface ServerConfig {
                            @Value.Default
                            default int maxThreads() {
                                return 100;
                            }

                            @Value.Immutable
                            interface Custom extends ServerConfig {
                                @Override
                                @Value.Default
                                default int maxThreads() {
                                    return 10;
                                }
                            }
                        }
                        """)
                .doTest();
    }

    @Test
    public void fixAddsDefaultWithoutOtherAnnotations() {
        RefactoringValidator.of(ImmutablesDefaultOverrideMissingAnnotation.class, getClass())
                .addInputLines(
                        "ServerConfig.java",
                        // language=Java
                        """
                        import org.immutables.value.Value;
                        interface ServerConfig {
                            @Value.Default
                            default int maxThreads() {
                                return 100;
                            }

                            @Value.Immutable
                            interface Custom extends ServerConfig {
                                default int maxThreads() {
                                    return 10;
                                }
                            }
                        }
                        """)
                .addOutputLines(
                        "ServerConfig.java",
                        // language=Java
                        """
                        import org.immutables.value.Value;
                        interface ServerConfig {
                            @Value.Default
                            default int maxThreads() {
                                return 100;
                            }

                            @Value.Immutable
                            interface Custom extends ServerConfig {
                                @Value.Default
                                default int maxThreads() {
                                    return 10;
                                }
                            }
                        }
                        """)
                .doTest();
    }

    private CompilationTestHelper helper() {
        return CompilationTestHelper.newInstance(ImmutablesDefaultOverrideMissingAnnotation.class, getClass());
    }
}
