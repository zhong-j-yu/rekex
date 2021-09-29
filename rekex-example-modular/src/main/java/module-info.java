
// we take the easy route, simply open the whole module to everybody, particulary
//   - rekex modules
//   - unittest module (unnamed)
// both requires reflective access to user module

open module org.rekex.example_modular
{
    requires org.rekex.parser;

    exports org.rekex.example_modular;
}