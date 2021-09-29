open module org.rekex.grammar
{
    requires transitive org.rekex.regexp;
    requires javassist;

    exports org.rekex.annomacro;
    exports org.rekex.annotype;
    exports org.rekex.grammar;
    exports org.rekex.helper.anno;
    exports org.rekex.helper.datatype;
    exports org.rekex.helper.datatype.seq;
    exports org.rekex.helper.datatype.alt;
    exports org.rekex.spec;
}