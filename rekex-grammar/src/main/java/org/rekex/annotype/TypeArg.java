package org.rekex.annotype;

// type argument
// wildcard is not supported now. our code assumes a TypeArg is a TypeRef.

/**
 * Type argument.
 * In the current version, wildcards are not supported as type arguments;
 * therefore a TypeArg is always a RefType.
 */
public sealed interface TypeArg permits RefType
{
    // @Deprecated
    // can't do this if we allow wildcards
    default RefType asRefType(){ return (RefType)this; }
}
