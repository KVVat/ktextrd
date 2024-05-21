import org.dom4j.Element
import org.dom4j.Node

//Helper fucntions for analyzing pdml files
fun Node.attrib(attrib:String="showname"):String{
    val elem = this as Element
    //println(elem)
    //println(elem.attrib(attrib))
    return if(elem.attribute(attrib) != null) {
        elem.attributeValue(attrib)
    } else {
        ""
    }
}
fun Node.selectChild(key:String): Node?
{
    //if(this == null) return null;
    return this.selectSingleNode(".//descendant::field[@name='${key}']")
}
fun Node.selectChildren(key:String):List<Node>?
{
    //if(this == null) return null;
    return this.selectNodes(".//descendant::field[@name='${key}']")
}

fun _showname(n: Node?):String{
    return n?.attrib() ?: "N/A"
}
fun _show(n: Node?):String{
    return n?.attrib("show") ?: "N/A"
}
fun _value(n: Node?):String {
    return n?.attrib("value") ?: "0"
}