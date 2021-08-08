package org.rekex.codegen;

import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

public class Imports
{
    final Set<ClassName> preImports;
    final Set<ClassName> imports;
    Imports(Set<ClassName> preImports, Set<ClassName> imports)
    {
        this.preImports = preImports;
        this.imports = imports;
    }

    public SortedSet<String> imports()
    {
        var list = imports.stream().map(ClassName::fullName).toList();
        return new TreeSet<>(list);
    }

    public String resolve(Class<?> clazz)
    {
        if(clazz.isPrimitive())
            return clazz.getCanonicalName();
        if(clazz.isArray())
            return resolve(clazz.getComponentType())+"[]";
        var cn = ClassName.parse(clazz);
        if(cn.pkg.equals("java.lang")) // java.lang.C.D -> C.D
            return String.join(".", cn.names);

        String sn;
        sn = tryResolve(preImports, cn);
        if(sn!=null)
            return sn;
        sn = tryResolve(imports, cn);
        if(sn!=null)
            return sn;
        return cn.fullName();
    }

    static String tryResolve(Set<ClassName> imports, ClassName cn)
    {
        String lastName = cn.lastName();
        if(imports.contains(cn))
            return lastName;
        if(cn.level()==0)
            return null;
        String sn0 = tryResolve(imports, cn.atLevel(cn.level()-1));
        if(sn0==null)
            return null;
        return sn0+"."+lastName;
    }

    record ClassName(String pkg, List<String> names)
    {
        static ClassName parse(Class<?> clazz)
        {
            if(clazz.isPrimitive() || clazz.isArray())
                throw new IllegalArgumentException("not a class: "+clazz);

            String pkg = clazz.getPackageName();
            if(pkg.isEmpty()) // not supported
                throw new IllegalArgumentException("unnamed package: "+clazz);

            String canonName = clazz.getCanonicalName(); // a.b.C.D
            if(canonName==null)
                throw new IllegalArgumentException("no canonical name: "+clazz);
            canonName = canonName.substring(pkg.length()+1); // C.D
            String[] names = canonName.split("\\.");
            return new ClassName(pkg, List.of(names));
        }

        // for pkg.C0.C1, level=1, C0 is its level 0 name, etc.
        int level()
        {
            return names.size()-1;
        }

        ClassName atLevel(int level)
        {
            return new ClassName(pkg, names.subList(0, level+1));
        }

        String lastName()
        {
            return names.get(names.size()-1);
        }

        String fullName()
        {
            return pkg + "." + String.join(".", names);
        }
    }



    public static Imports build(Set<Class<?>> preImported, Set<Class<?>> candidates)
    {
        // `imported`: init as
        //     all public classes in java.lang package
        //     all classes in preImported
        //
        // for level from 0 to max
        //    collect distinct classes at that level from candidates,
        //    collect `names` of last names of those classes,
        //    if a name is unique in `names`, and it's not in `imported`
        //    add it to `imported`
        //
        // say for candidate pkg.C0$C1, both pkg.C0, pkg.C0.C1 are now imported,
        // but pkg.C0 is never referenced in program; and the name C0 could've been
        // imported for another candidate pkg2.D.C0. We don't do that.
        // When C1 is referenced in program, user is aware of it's enclosing C0.
        // If C0 means something else, it's quite confusing.
        //
        // later we do remove pkg.C0 from imports if it's unused (i.e. not in `candidates`,
        // which we assume contains all classes used in program besides preImported.)

        HashSet<String> imported = new HashSet<>();
        for(var clazz : preImported)
            imported.add(ClassName.parse(clazz).lastName());
        // note that it doesn't contain java.lang classes, since we can't enum them.

        HashSet<ClassName> imports = new HashSet<>();

        Set<ClassName> cnList0 = candidates.stream().map(ClassName::parse).collect(Collectors.toSet());
        HashSet<ClassName> cnList = new HashSet<>(cnList0);

        for(int level=0;; level++)
        {
            final var levelF = level;
            cnList.removeIf(cn->cn.level()<levelF);
            if(cnList.isEmpty())
                break;

            Map<String,Long> nameCount = cnList.stream()
                .map(cn->cn.atLevel(levelF))
                .distinct()
                .map(cn->cn.names.get(levelF))
                .collect(Collectors.groupingBy(n->n, Collectors.counting()));

            for(var cn : cnList)
            {
                var name = cn.names.get(level);
                if(nameCount.get(name)==1 && !imported.contains(name) && !in_java_lang(name))
                {
                    imported.add(name);
                    imports.add(cn.atLevel(level));
                }
            }
        }

        // remove unused imports
        imports.removeIf(cn->!cnList0.contains(cn));

        Set<ClassName> preImports = preImported.stream().map(ClassName::parse).collect(Collectors.toSet());
        return new Imports(preImports, imports);
    }

    static boolean in_java_lang(String name)
    {
        Class<?> clazz;
        try
        {
            clazz = Class.forName("java.lang."+name);
        }
        catch (ClassNotFoundException e)
        {
            return false;
        }
        return Modifier.isPublic(clazz.getModifiers());
    }
}
